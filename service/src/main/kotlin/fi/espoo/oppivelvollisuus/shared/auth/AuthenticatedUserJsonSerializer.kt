// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.auth

import fi.espoo.oppivelvollisuus.shared.utils.exhaust
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

// Custom serializer to avoid Jackson serializing "fields" that are actually helper functions (e.g.
// isAdmin)
class AuthenticatedUserJsonSerializer : ValueSerializer<AuthenticatedUser>() {
    override fun serialize(
        value: AuthenticatedUser,
        gen: JsonGenerator,
        ctxt: SerializationContext,
    ) {
        gen.writeStartObject()
        gen.writePOJOProperty("type", value.type.toString())
        when (value) {
            is AuthenticatedUser.ProviderUser -> {
                gen.writePOJOProperty("id", value.id.toString())
            }

            is AuthenticatedUser.EspooUser -> {
                gen.writePOJOProperty("id", value.id.toString())
            }

            is AuthenticatedUser.SystemInternalUser -> {}
        }.exhaust()
        gen.writeEndObject()
    }
}
