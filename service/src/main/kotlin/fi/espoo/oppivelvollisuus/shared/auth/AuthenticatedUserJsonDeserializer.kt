// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.auth

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import fi.espoo.oppivelvollisuus.EspooUserId
import java.util.UUID

class AuthenticatedUserJsonDeserializer : JsonDeserializer<AuthenticatedUser>() {
    private data class AllFields(
        val type: AuthenticatedUserType? = null,
        val id: UUID? = null
    )

    override fun deserialize(
        p: JsonParser,
        ctx: DeserializationContext
    ): AuthenticatedUser {
        val user = p.readValueAs(AllFields::class.java)
        return when (user.type!!) {
            AuthenticatedUserType.espooUser -> {
                AuthenticatedUser.EspooUser(EspooUserId(user.id!!))
            }

            AuthenticatedUserType.system -> {
                AuthenticatedUser.SystemInternalUser
            }
        }
    }
}
