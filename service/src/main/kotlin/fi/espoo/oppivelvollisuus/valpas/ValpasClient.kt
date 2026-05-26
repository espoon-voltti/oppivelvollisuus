// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import java.time.LocalDate
import java.util.UUID

sealed interface ValpasQueryStatus {
    data object Pending : ValpasQueryStatus

    data object Running : ValpasQueryStatus

    data class Complete(val fileUrls: List<String>) : ValpasQueryStatus

    data object Failed : ValpasQueryStatus
}

data class ValpasOppija(
    val oppijanumero: String,
    val etunimet: String,
    val sukunimi: String,
    val syntymäaika: LocalDate?,
    val hetu: String?,
    val aktiivinenKuntailmoitus: ValpasKuntailmoitus?,
)

data class ValpasKuntailmoitus(
    val id: UUID,
    val aikaleima: LocalDate?,
    val oppijanYhteystiedot: ValpasYhteystiedot?,
    val onUudempiaIlmoituksiaMuihinKuntiin: Boolean? = null,
)

data class ValpasYhteystiedot(
    val puhelinnumero: String?,
    val email: String?,
    val lähiosoite: String?,
    val postinumero: String?,
    val postitoimipaikka: String?,
)

data class ValpasResultFile(val oppijat: List<ValpasOppija>)

class ValpasIntegrationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

interface ValpasClient {
    /** Starts a new Valpas mass-luovutus query. Returns the queryId. */
    fun startQuery(): String

    /** Polls the status of an existing query. */
    fun getQueryStatus(queryId: String): ValpasQueryStatus

    /** Downloads and parses one result file. */
    fun downloadResultFile(url: String): ValpasResultFile
}
