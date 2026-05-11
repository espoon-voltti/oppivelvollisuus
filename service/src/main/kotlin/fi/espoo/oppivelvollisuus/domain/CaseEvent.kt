// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.CaseEventId
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.shared.NotFound
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.time.ZonedDateTime

enum class CaseEventType {
    NOTE,
    TEXT_MESSAGE,
    EMAIL,
    PHONE_CALL,
    INTRO_VISIT,
    MEETING,
    CANCELLED_MEETING,
    EXPLANATION_REQUEST,
    EDUCATION_SUSPENSION_APPLICATION_RECEIVED,
    EDUCATION_SUSPENSION_GRANTED,
    EDUCATION_SUSPENSION_DENIED,
    CHILD_PROTECTION_NOTICE,
    HEARING_LETTER,
    HEARING,
    DIRECTED_TO_YLEISOPPILAITOKSEN_TUVA,
    DIRECTED_TO_ERITYISOPPILAITOKSEN_TUVA,
    DIRECTED_TO_ERITYISOPPILAITOKSEN_TELMA
}

data class CaseEventInput(
    val date: LocalDate,
    val type: CaseEventType,
    val notes: String
)

fun Database.Transaction.insertCaseEvent(
    studentCaseId: StudentCaseId,
    data: CaseEventInput,
    user: AuthenticatedUser
): CaseEventId =
    handle.createUpdate(
        """
                INSERT INTO case_events (created_by, student_case_id, date, type, notes)
                VALUES (:user, :studentCaseId, :date, :type, :notes)
                RETURNING id
            """
    ).bind("studentCaseId", studentCaseId.raw)
        .bindKotlin(data)
        .bind("user", user.rawId())
        .executeAndReturnGeneratedKeys()
        .mapTo<CaseEventId>()
        .one()

data class CaseEvent(
    val id: CaseEventId,
    val studentCaseId: StudentCaseId,
    val date: LocalDate,
    val type: CaseEventType,
    val notes: String,
    val created: ModifyInfo,
    val updated: ModifyInfo?
)

data class ModifyInfo(
    val name: String,
    val time: ZonedDateTime
)

fun Database.Transaction.updateCaseEvent(
    id: CaseEventId,
    data: CaseEventInput,
    user: AuthenticatedUser
) {
    handle.createUpdate(
        """
UPDATE case_events
SET
    updated = now(),
    updated_by = :user,
    date = :date,
    type = :type,
    notes = :notes
WHERE id = :id
"""
    ).bind("id", id.raw)
        .bindKotlin(data)
        .bind("user", user.rawId())
        .execute()
        .also { if (it != 1) throw NotFound() }
}

fun Database.Transaction.deleteCaseEvent(id: CaseEventId) {
    handle.createUpdate(
        """
DELETE FROM case_events
WHERE id = :id
"""
    ).bind("id", id.raw)
        .execute()
}
