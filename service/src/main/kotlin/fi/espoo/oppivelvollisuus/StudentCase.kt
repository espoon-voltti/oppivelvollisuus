package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.*

data class StudentCaseInput(
    val openedAt: LocalDate,
    val info: String
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
    val info: String
)

fun Handle.getStudentCasesByStudent(studentId: UUID): List<StudentCase> = createQuery(
"""
SELECT id, student_id, opened_at, info
FROM student_cases
WHERE student_id = :studentId
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
    info = :info
WHERE id = :id AND student_id = :studentId
"""
    )
        .bind("id", id)
        .bind("studentId", studentId)
        .bindKotlin(data)
        .execute()
        .also { if (it != 1) error("not found") }
}
