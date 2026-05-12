// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// Phase 2 shim — DELETE in Phase 3 once domain code uses the sealed type directly.
package fi.espoo.oppivelvollisuus.config

import fi.espoo.oppivelvollisuus.shared.config.getAuthenticatedUser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.UUID
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser as SharedAuthenticatedUser

/**
 * Legacy AuthenticatedUser data class — Phase 2 shim.
 *
 * Domain code (AppController, Student, StudentCase, CaseEvent, Audit) still compiles against this
 * type and accesses `.id: UUID`. Phase 3 will rewrite each callsite to use the sealed
 * `SharedAuthenticatedUser` directly and delete this shim.
 */
data class AuthenticatedUser(
    val id: UUID
) {
    fun isSystemUser() = id == UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun toShared(): SharedAuthenticatedUser =
        if (isSystemUser()) {
            SharedAuthenticatedUser.SystemInternalUser
        } else {
            SharedAuthenticatedUser.EspooUser(fi.espoo.oppivelvollisuus.EspooUserId(id))
        }
}

/**
 * Converts the sealed SharedAuthenticatedUser (placed in the request attribute by
 * RequestToAuthenticatedUser) into the legacy AuthenticatedUser(id: UUID) for injection into
 * controller methods that still use the old type.
 */
@Configuration
class LegacyAuthenticatedUserMvcConfig : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(LegacyAuthenticatedUserResolver)
    }
}

object LegacyAuthenticatedUserResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        AuthenticatedUser::class.java.isAssignableFrom(parameter.parameterType)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AuthenticatedUser? {
        val sharedUser =
            webRequest.getNativeRequest(HttpServletRequest::class.java)?.getAuthenticatedUser()
        if (sharedUser == null && !parameter.isOptional) {
            error("Unauthorized request (${webRequest.getDescription(false)})")
        }
        return sharedUser?.toLegacy()
    }
}

/** Convert shared sealed AuthenticatedUser → legacy AuthenticatedUser(id: UUID). */
fun SharedAuthenticatedUser.toLegacy(): AuthenticatedUser = AuthenticatedUser(id = rawId())
