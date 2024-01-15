// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.common.NotFound
import fi.espoo.oppivelvollisuus.config.AuthenticatedUser
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

enum class CaseEventType {
    NOTE,
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

fun Handle.insertCaseEvent(
    studentCaseId: UUID,
    data: CaseEventInput,
    user: AuthenticatedUser
): UUID {
    return createUpdate(
        """
                INSERT INTO case_events (created_by, student_case_id, date, type, notes) 
                VALUES (:user, :studentCaseId, :date, :type, :notes)
                RETURNING id
            """
    )
        .bind("studentCaseId", studentCaseId)
        .bindKotlin(data)
        .bind("user", user.id)
        .executeAndReturnGeneratedKeys()
        .mapTo<UUID>()
        .one()
}

data class CaseEvent(
    val id: UUID,
    val studentCaseId: UUID,
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

fun Handle.updateCaseEvent(
    id: UUID,
    data: CaseEventInput,
    user: AuthenticatedUser
) {
    createUpdate(
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
    )
        .bind("id", id)
        .bindKotlin(data)
        .bind("user", user.id)
        .execute()
        .also { if (it != 1) throw NotFound() }
}

fun Handle.deleteCaseEvent(id: UUID) {
    createUpdate(
        """
DELETE FROM case_events
WHERE id = :id
"""
    )
        .bind("id", id)
        .execute()
}
