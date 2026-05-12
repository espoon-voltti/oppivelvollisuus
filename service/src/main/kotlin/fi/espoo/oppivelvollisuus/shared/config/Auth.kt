// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.config

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.oppivelvollisuus.EspooUserId
import fi.espoo.oppivelvollisuus.shared.Tracing
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.config.defaultJsonMapper
import fi.espoo.oppivelvollisuus.shared.logging.MdcKey
import fi.espoo.oppivelvollisuus.shared.setAttribute
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.trace.Span
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID

class HttpAccessControl : HttpFilter() {
    override fun doFilter(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        if (request.requiresAuthentication()) {
            val authenticatedUser = request.getAuthenticatedUser()
            if (authenticatedUser == null) {
                return response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "fi.espoo.oppivelvollisuus.shared.Unauthorized",
                )
            }
            if (!request.isAuthorized(authenticatedUser)) {
                return response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "fi.espoo.oppivelvollisuus.shared.Forbidden",
                )
            }
        }

        chain.doFilter(request, response)
    }

    private fun HttpServletRequest.requiresAuthentication(): Boolean =
        when {
            requestURI == "/health" || requestURI == "/actuator/health" -> false
            else -> true
        }

    private fun HttpServletRequest.isAuthorized(user: AuthenticatedUser): Boolean =
        when {
            requestURI.startsWith("/system/") -> user is AuthenticatedUser.SystemInternalUser
            // All other authenticated endpoints allow any non-system user
            else -> user !is AuthenticatedUser.SystemInternalUser
        }
}

class JwtTokenDecoder(
    private val jwtVerifier: JWTVerifier
) : HttpFilter() {
    private val logger = KotlinLogging.logger {}

    override fun doFilter(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        try {
            request
                .getBearerToken()
                ?.takeIf { it.isNotEmpty() }
                ?.let { request.setDecodedJwt(jwtVerifier.verify(it)) }
        } catch (e: JWTVerificationException) {
            logger.error(e) { "JWT token verification failed" }
        }
        chain.doFilter(request, response)
    }
}

private const val ATTR_USER = "oppivelvollisuus.user"

fun HttpServletRequest.getAuthenticatedUser(): AuthenticatedUser? = getAttribute(ATTR_USER) as AuthenticatedUser?

fun HttpServletRequest.setAuthenticatedUser(user: AuthenticatedUser) = setAttribute(ATTR_USER, user)

private const val ATTR_JWT = "oppivelvollisuus.jwt"

private fun HttpServletRequest.getDecodedJwt(): DecodedJWT? = getAttribute(ATTR_JWT) as DecodedJWT?

private fun HttpServletRequest.setDecodedJwt(jwt: DecodedJWT) = setAttribute(ATTR_JWT, jwt)

private fun HttpServletRequest.getBearerToken(): String? = getHeader("Authorization")?.substringAfter("Bearer ", missingDelimiterValue = "")

private val systemUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

class RequestToAuthenticatedUser : HttpFilter() {
    override fun doFilter(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val decodedJwt = request.getDecodedJwt()
        if (decodedJwt != null) {
            // JWT is valid => the request came from apigw.
            // Prefer X-User header (JSON-encoded AuthenticatedUser) if present; otherwise fall back
            // to JWT.subject (a UUID). The fallback supports the oppivelvollisuus apigw flow which
            // does not inject an X-User header — it encodes the user identity solely in the JWT
            // subject claim.
            val user =
                request.getHeader("X-User")?.let { defaultJsonMapper().readValue<AuthenticatedUser>(it) }
                    ?: decodedJwt.subject?.let { subject ->
                        val id = UUID.fromString(subject)
                        if (id == systemUserId) {
                            AuthenticatedUser.SystemInternalUser
                        } else {
                            AuthenticatedUser.EspooUser(EspooUserId(id))
                        }
                    }
            if (user != null) {
                request.setAuthenticatedUser(user)
                Span.current().setAttribute(Tracing.enduserIdHash, user.rawIdHash)
                MdcKey.USER_ID.set(user.rawId().toString())
                MdcKey.USER_ID_HASH.set(user.rawIdHash.toString())
            }
        }
        try {
            chain.doFilter(request, response)
        } finally {
            MdcKey.SECONDARY_USER_ID_HASH.unset()
            MdcKey.USER_ID_HASH.unset()
            MdcKey.USER_ID.unset()
        }
    }
}
