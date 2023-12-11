package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.common.NotFound
import fi.espoo.oppivelvollisuus.config.AuthenticatedUser
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

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
    @Nested("created") val created: ModifyInfo,
    @Nested("updated") val updated: ModifyInfo?
)

data class ModifyInfo(
    @PropagateNull val name: String,
    @PropagateNull val time: ZonedDateTime
)

fun Handle.getCaseEventsByStudentCase(studentCaseId: UUID): List<CaseEvent> = createQuery(
"""
SELECT sc.id, sc.student_case_id, sc.date, sc.type, sc.notes,
    creator.first_name || ' ' || creator.last_name AS created_name,
    sc.created AS created_time,
    CASE WHEN updater.id IS NOT NULL THEN updater.first_name || ' ' || updater.last_name END AS updated_name,
    sc.updated AS updated_time
FROM case_events sc
JOIN users creator ON sc.created_by = creator.id
LEFT JOIN users updater ON sc.updated_by = updater.id
WHERE student_case_id = :studentCaseId
ORDER BY date DESC, sc.created DESC 
"""
)
    .bind("studentCaseId", studentCaseId)
    .mapTo<CaseEvent>()
    .list()

fun Handle.updateCaseEvent(id: UUID, data: CaseEventInput, user: AuthenticatedUser) {
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
