// SPDX-FileCopyrightText: 2025-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.domain.StudentInput
import fi.espoo.oppivelvollisuus.domain.deleteStudentCase
import fi.espoo.oppivelvollisuus.domain.existsCaseWithNotificationId
import fi.espoo.oppivelvollisuus.domain.findImportedFromValpasCaseForStudent
import fi.espoo.oppivelvollisuus.domain.findStudentIdBySsn
import fi.espoo.oppivelvollisuus.domain.insertImportedCase
import fi.espoo.oppivelvollisuus.domain.insertStudent
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DuplicateKeyException

private val logger = KotlinLogging.logger {}

fun buildAddressString(y: ValpasYhteystiedot?): String {
    if (y == null) return ""
    return listOfNotNull(
            y.lähiosoite?.takeIf { it.isNotBlank() },
            y.postinumero?.takeIf { it.isNotBlank() },
            y.postitoimipaikka?.takeIf { it.isNotBlank() },
        )
        .joinToString(separator = ", ")
}

fun mapToStudentInput(oppija: ValpasOppija, opintopolkuBaseUrl: String?): StudentInput {
    val notification = oppija.aktiivinenKuntailmoitus
    val valpasLink =
        if (opintopolkuBaseUrl != null) {
            "${opintopolkuBaseUrl.trimEnd('/')}/valpas/virkailija/oppija/${oppija.oppijanumero}"
        } else {
            ""
        }
    return StudentInput(
        valpasLink = valpasLink,
        valpasOppijaOid = oppija.oppijanumero,
        ssn = oppija.hetu ?: "",
        firstName = oppija.etunimet,
        lastName = oppija.sukunimi,
        language = "",
        dateOfBirth =
            requireNotNull(oppija.syntymäaika) { "syntymäaika required to insert student" },
        phone = notification?.oppijanYhteystiedot?.puhelinnumero ?: "",
        email = notification?.oppijanYhteystiedot?.email ?: "",
        gender = null,
        address = buildAddressString(notification?.oppijanYhteystiedot),
        municipalityInFinland = true,
        guardianInfo = "",
        supportContactsInfo = "",
        partnerOrganisations = emptySet(),
    )
}

fun findOrInsertStudentForImport(
    tx: Database.Transaction,
    oppija: ValpasOppija,
    opintopolkuBaseUrl: String?,
    now: HelsinkiDateTime,
): StudentId {
    val hetu = requireNotNull(oppija.hetu) { "hetu is required" }
    return tx.findStudentIdBySsn(hetu)
        ?: tx.insertStudent(
            data = mapToStudentInput(oppija, opintopolkuBaseUrl),
            createdBy = AuthenticatedUser.SystemInternalUser.espooUserId,
            now = now,
        )
}

fun importValpasOppija(
    tx: Database.Transaction,
    oppija: ValpasOppija,
    opintopolkuBaseUrl: String?,
    now: HelsinkiDateTime,
): StudentCaseId? {
    val notification = oppija.aktiivinenKuntailmoitus
    if (notification == null) {
        logger.warn { "Skipping oppija ${oppija.oppijanumero}: no aktiivinenKuntailmoitus" }
        return null
    }
    if (oppija.hetu.isNullOrBlank()) {
        logger.warn { "Skipping oppija ${oppija.oppijanumero}: missing hetu" }
        return null
    }
    if (oppija.syntymäaika == null) {
        logger.warn { "Skipping oppija ${oppija.oppijanumero}: missing syntymäaika" }
        return null
    }

    if (tx.existsCaseWithNotificationId(notification.id)) {
        return null
    }

    val studentId = findOrInsertStudentForImport(tx, oppija, opintopolkuBaseUrl, now)

    val existingImported = tx.findImportedFromValpasCaseForStudent(studentId)
    if (existingImported != null) {
        logger.error {
            "Replacing existing IMPORTED_FROM_VALPAS case $existingImported " +
                "for student $studentId with newer notification ${notification.id}"
        }
        tx.deleteStudentCase(existingImported, studentId)
    }

    val openedAt = notification.aikaleima ?: now.toLocalDate()
    return try {
        tx.insertImportedCase(
            studentId = studentId,
            valpasNotificationId = notification.id,
            openedAt = openedAt,
            now = now,
        )
    } catch (e: DuplicateKeyException) {
        logger.error(e) { "Concurrent insert won the race for notification ${notification.id}" }
        null
    }
}
