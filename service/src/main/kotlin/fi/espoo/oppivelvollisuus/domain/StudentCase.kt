package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.UserBasics
import fi.espoo.oppivelvollisuus.config.AuthenticatedUser
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import java.time.LocalDate
import java.util.*

data class StudentCaseInput(
    val openedAt: LocalDate,
    val assignedTo: UUID?
)

fun Handle.insertStudentCase(
    studentId: UUID,
    data: StudentCaseInput,
    user: AuthenticatedUser
): UUID {
    return createUpdate(
        """
                INSERT INTO student_cases (created_by, student_id, opened_at, assigned_to) 
                VALUES (:user, :studentId, :openedAt, :assignedTo)
                RETURNING id
            """
    )
        .bind("studentId", studentId)
        .bindKotlin(data)
        .bind("user", user.id)
        .executeAndReturnGeneratedKeys()
        .mapTo<UUID>()
        .one()
}

data class StudentCase(
    val id: UUID,
    val studentId: UUID,
    val openedAt: LocalDate,
    @Nested("assignedTo") val assignedTo: UserBasics?
)

fun Handle.getStudentCasesByStudent(studentId: UUID): List<StudentCase> = createQuery(
"""
SELECT 
    sc.id, sc.student_id, sc.opened_at, 
    assignee.id AS assigned_to_id, 
    assignee.first_name || ' ' || assignee.last_name AS assigned_to_name
FROM student_cases sc
LEFT JOIN users assignee ON sc.assigned_to = assignee.id
WHERE student_id = :studentId
ORDER BY opened_at DESC, sc.created DESC 
"""
)
    .bind("studentId", studentId)
    .mapTo<StudentCase>()
    .list()

fun Handle.updateStudentCase(id: UUID, studentId: UUID, data: StudentCaseInput, user: AuthenticatedUser) {
    createUpdate(
"""
UPDATE student_cases
SET 
    updated = now(),
    updated_by = :user,
    opened_at = :openedAt,
    assigned_to = :assignedTo
WHERE id = :id AND student_id = :studentId
"""
    )
        .bind("id", id)
        .bind("studentId", studentId)
        .bindKotlin(data)
        .bind("user", user.id)
        .execute()
        .also { if (it != 1) error("not found") }
}
