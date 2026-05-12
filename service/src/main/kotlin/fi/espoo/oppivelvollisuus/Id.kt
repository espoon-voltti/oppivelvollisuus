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
    sealed class Attachment : DatabaseTable

    sealed class AttachmentType : DatabaseTable

    sealed class CompanyAcl : DatabaseTable

    sealed class Company : DatabaseTable

    sealed class Daycare : DatabaseTable

    sealed class DaycareDecision : DatabaseTable

    sealed class EspooUser : DatabaseTable

    sealed class PriceCatalogue : DatabaseTable

    sealed class PriceCatalogueRow : DatabaseTable

    sealed class ProviderUser : DatabaseTable

    sealed class ServiceOption : DatabaseTable
}

typealias AttachmentId = Id<DatabaseTable.Attachment>

typealias AttachmentTypeId = Id<DatabaseTable.AttachmentType>

typealias CompanyAclId = Id<DatabaseTable.CompanyAcl>

typealias CompanyId = Id<DatabaseTable.Company>

typealias DaycareDecisionId = Id<DatabaseTable.DaycareDecision>

typealias DaycareId = Id<DatabaseTable.Daycare>

typealias EspooUserId = Id<DatabaseTable.EspooUser>

typealias PriceCatalogueId = Id<DatabaseTable.PriceCatalogue>

typealias PriceCatalogueRowId = Id<DatabaseTable.PriceCatalogueRow>

typealias ProviderUserId = Id<DatabaseTable.ProviderUser>

typealias ServiceOptionId = Id<DatabaseTable.ServiceOption>

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
