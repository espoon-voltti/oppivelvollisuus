// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.KeyDeserializer
import tools.jackson.databind.annotation.JsonDeserialize

sealed interface DatabaseTable {
    sealed class User : DatabaseTable

    // TODO: Add others
}

typealias EspooUserId = Id<DatabaseTable.User>

// TODO: Add others

@JsonDeserialize(keyUsing = Id.KeyFromJson::class)
data class Id<out T : DatabaseTable>(val raw: UUID) : Comparable<Id<*>> {
    @JsonValue override fun toString(): String = raw.toString()

    override fun compareTo(other: Id<*>): Int = this.raw.compareTo(other.raw)

    companion object {
        @JvmStatic
        @JsonCreator
        fun <T : DatabaseTable> fromString(value: String): Id<T> = Id(UUID.fromString(value))
    }

    class KeyFromJson : KeyDeserializer() {
        override fun deserializeKey(key: String, ctxt: DeserializationContext): Any =
            Id<DatabaseTable>(UUID.fromString(key))
    }
}
