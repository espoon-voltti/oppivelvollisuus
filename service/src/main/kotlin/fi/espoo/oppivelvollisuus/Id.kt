// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.util.UUID

sealed interface DatabaseTable {
    sealed class EspooUser : DatabaseTable

    // oppivelvollisuus domain tables
    sealed class Student : DatabaseTable

    sealed class StudentCase : DatabaseTable

    sealed class CaseEvent : DatabaseTable
}

typealias EspooUserId = Id<DatabaseTable.EspooUser>

typealias StudentId = Id<DatabaseTable.Student>

typealias StudentCaseId = Id<DatabaseTable.StudentCase>

typealias CaseEventId = Id<DatabaseTable.CaseEvent>

@JsonDeserialize(keyUsing = Id.KeyFromJson::class)
data class Id<out T : DatabaseTable>(
    val raw: UUID
) : Comparable<Id<*>> {
    @JsonValue override fun toString(): String = raw.toString()

    override fun compareTo(other: Id<*>): Int = this.raw.compareTo(other.raw)

    companion object {
        @JvmStatic
        @JsonCreator
        fun <T : DatabaseTable> fromString(value: String): Id<T> = Id(UUID.fromString(value))
    }

    class KeyFromJson : KeyDeserializer() {
        override fun deserializeKey(
            key: String,
            ctxt: DeserializationContext
        ): Any = Id<DatabaseTable>(UUID.fromString(key))
    }
}
