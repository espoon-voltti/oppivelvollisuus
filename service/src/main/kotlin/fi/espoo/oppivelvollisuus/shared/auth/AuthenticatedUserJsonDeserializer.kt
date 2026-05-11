// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.auth

import fi.espoo.oppivelvollisuus.EspooUserId
import fi.espoo.oppivelvollisuus.ProviderUserId
import java.util.UUID
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

class AuthenticatedUserJsonDeserializer : ValueDeserializer<AuthenticatedUser>() {
    private data class AllFields(val type: AuthenticatedUserType? = null, val id: UUID? = null)

    override fun deserialize(p: JsonParser, ctx: DeserializationContext): AuthenticatedUser {
        val user = p.readValueAs(AllFields::class.java)
        return when (user.type!!) {
            AuthenticatedUserType.providerUser -> {
                AuthenticatedUser.ProviderUser(ProviderUserId(user.id!!))
            }

            AuthenticatedUserType.espooUser -> {
                AuthenticatedUser.EspooUser(EspooUserId(user.id!!))
            }

            AuthenticatedUserType.system -> {
                AuthenticatedUser.SystemInternalUser
            }
        }
    }
}
