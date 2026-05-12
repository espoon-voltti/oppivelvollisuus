// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.config

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.readValue

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
                    "fi.espoo.oppivelvollisuus.common.Unauthorized",
                )
            }
            if (!request.isAuthorized(authenticatedUser)) {
                return response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "fi.espoo.oppivelvollisuus.common.Forbidden",
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
            else -> user is AuthenticatedUser.EspooUser
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

class RequestToAuthenticatedUser : HttpFilter() {
    override fun doFilter(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val decodedJwt = request.getDecodedJwt()
        if (decodedJwt != null) {
            // JWT is valid => the request came from apigw
            val user =
                request.getHeader("X-User")?.let { jsonMapper().readValue<AuthenticatedUser>(it) }
            if (user != null) {
                request.setAuthenticatedUser(user)
                // TODO: tag the request with MDC user keys and an OpenTelemetry span attribute for the authenticated user, as the other Voltti projects do, once that infrastructure exists in this service
            }
        }
        chain.doFilter(request, response)
    }
}
