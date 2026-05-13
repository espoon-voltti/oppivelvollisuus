// SPDX-FileCopyrightText: 2026-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.config

import com.google.common.hash.HashCode
import com.google.common.hash.Hashing
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize
import java.util.UUID

@JsonSerialize(using = AuthenticatedUserJsonSerializer::class)
@JsonDeserialize(using = AuthenticatedUserJsonDeserializer::class)
sealed class AuthenticatedUser {
    abstract val type: AuthenticatedUserType

    abstract fun rawId(): UUID

    val rawIdHash: HashCode
        get() = Hashing.sha256().hashString(rawId().toString(), Charsets.UTF_8)

    data class EspooUser(
        val id: UUID
    ) : AuthenticatedUser() {
        override fun rawId(): UUID = id

        override val type = AuthenticatedUserType.espooUser
    }

    data object SystemInternalUser : AuthenticatedUser() {
        override fun rawId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        override val type = AuthenticatedUserType.system

        override fun toString(): String = "SystemInternalUser"
    }
}

/** Low-level AuthenticatedUser type "tag" used in serialized representations (JWT, JSON). */
@Suppress("EnumEntryName", "ktlint:standard:enum-entry-name-case")
enum class AuthenticatedUserType {
    espooUser,
    system,
}
