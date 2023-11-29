package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import java.time.LocalDate
import java.util.*

data class StudentCaseInput(
    val openedAt: LocalDate,
    val info: String,
    val assignedTo: String?
)

fun Handle.insertStudentCase(
    id: UUID,
    studentId: UUID,
    data: StudentCaseInput
) {
    createUpdate(
        """
                INSERT INTO student_cases (id, student_id, opened_at, info) 
                VALUES (:id, :studentId, :openedAt, :info)
            """
    )
        .bind("id", id)
        .bind("studentId", studentId)
        .bindKotlin(data)
        .execute()
}

data class StudentCase(
    val id: UUID,
    val studentId: UUID,
    val openedAt: LocalDate,
    val info: String,
    @Nested("assignedTo") val assignedTo: EmployeeBasics?
)

fun Handle.getStudentCasesByStudent(studentId: UUID): List<StudentCase> = createQuery(
"""
SELECT 
    id, student_id, opened_at, info, 
    assignee.external_id AS assigned_to_id, 
    assignee.first_name || ' ' || assignee.last_name AS assigned_to_name
FROM student_cases
LEFT JOIN employees assignee ON student_cases.assigned_to = assignee.external_id
WHERE student_id = :studentId
ORDER BY opened_at DESC, student_cases.created DESC 
"""
)
    .bind("studentId", studentId)
    .mapTo<StudentCase>()
    .list()

fun Handle.updateStudentCase(id: UUID, studentId: UUID, data: StudentCaseInput) {
    createUpdate(
"""
UPDATE student_cases
SET 
    opened_at = :openedAt,
    info = :info,
    assigned_to = :assignedTo
WHERE id = :id AND student_id = :studentId
"""
    )
        .bind("id", id)
        .bind("studentId", studentId)
        .bindKotlin(data)
        .execute()
        .also { if (it != 1) error("not found") }
}
