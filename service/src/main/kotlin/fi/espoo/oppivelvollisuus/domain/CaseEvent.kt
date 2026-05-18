// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.CaseEventId
import fi.espoo.oppivelvollisuus.EspooUserId
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.db.DatabaseEnum
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import java.time.LocalDate
import java.time.ZonedDateTime

enum class CaseEventType : DatabaseEnum {
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
    DIRECTED_TO_ERITYISOPPILAITOKSEN_TELMA;

    override val sqlType: String = "case_event_type"
}

data class CaseEventInput(val date: LocalDate, val type: CaseEventType, val notes: String)

fun Database.Transaction.insertCaseEvent(
    studentCaseId: StudentCaseId,
    data: CaseEventInput,
    createdBy: EspooUserId,
    now: HelsinkiDateTime,
): CaseEventId =
    createUpdate {
            sql(
                """
                INSERT INTO case_events (created, created_by, student_case_id, date, type, notes)
                VALUES (${bind(now)}, ${bind(createdBy)}, ${bind(studentCaseId)}, ${bind(data.date)}, ${bind(data.type)}, ${bind(data.notes)})
                RETURNING id
                """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<CaseEventId>()

data class CaseEvent(
    val id: CaseEventId,
    val studentCaseId: StudentCaseId,
    val date: LocalDate,
    val type: CaseEventType,
    val notes: String,
    val created: ModifyInfo,
    val updated: ModifyInfo?,
)

data class ModifyInfo(val name: String, val time: ZonedDateTime)

fun Database.Transaction.updateCaseEvent(
    id: CaseEventId,
    data: CaseEventInput,
    updatedBy: EspooUserId,
    now: HelsinkiDateTime,
) {
    createUpdate {
            sql(
                """
                UPDATE case_events
                SET
                    updated = ${bind(now)},
                    updated_by = ${bind(updatedBy)},
                    date = ${bind(data.date)},
                    type = ${bind(data.type)},
                    notes = ${bind(data.notes)}
                WHERE id = ${bind(id)}
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.deleteCaseEvent(id: CaseEventId) {
    createUpdate { sql("DELETE FROM case_events WHERE id = ${bind(id)}") }.execute()
}
