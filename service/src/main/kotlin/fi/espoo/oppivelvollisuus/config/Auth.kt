package fi.espoo.oppivelvollisuus.config

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging

data class AuthenticatedUser(
    val externalId: String
) {
    fun isSystemUser() = externalId == "oppivelvollisuus-system-user"
}

class JwtToAuthenticatedUser : HttpFilter() {
    override fun doFilter(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val user = request.getDecodedJwt()?.subject?.let { subject ->
            AuthenticatedUser(externalId = subject)
        }
        if (user != null) {
            request.setAttribute(ATTR_USER, user)
        }
        chain.doFilter(request, response)
    }
}

class HttpAccessControl : HttpFilter() {
    override fun doFilter(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        if (request.requiresAuthentication()) {
            val authenticatedUser = request.getAuthenticatedUser()
            if (authenticatedUser == null) {
                return response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
            }
            if (!request.isAuthorized(authenticatedUser)) {
                return response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")
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
            requestURI.startsWith("/system/") -> user.isSystemUser()
            else -> true
        }
}

class JwtTokenDecoder(private val jwtVerifier: JWTVerifier) : HttpFilter() {
    private val logger = KotlinLogging.logger {}

    override fun doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        try {
            request.getBearerToken()
                ?.takeIf { it.isNotEmpty() }
                ?.let { request.setDecodedJwt(jwtVerifier.verify(it)) }
        } catch (e: JWTVerificationException) {
            logger.error(e) { "JWT token verification failed" }
        }
        chain.doFilter(request, response)
    }
}

fun HttpServletRequest.getAuthenticatedUser(): AuthenticatedUser? =
    getAttribute(ATTR_USER) as AuthenticatedUser?

private const val ATTR_USER = "oppivelvollisuus.user"
private const val ATTR_JWT = "oppivelvollisuus.jwt"

private fun HttpServletRequest.getDecodedJwt(): DecodedJWT? = getAttribute(ATTR_JWT) as DecodedJWT?

private fun HttpServletRequest.setDecodedJwt(jwt: DecodedJWT) = setAttribute(ATTR_JWT, jwt)

private fun HttpServletRequest.getBearerToken(): String? =
    getHeader("Authorization")?.substringAfter("Bearer ", missingDelimiterValue = "")
