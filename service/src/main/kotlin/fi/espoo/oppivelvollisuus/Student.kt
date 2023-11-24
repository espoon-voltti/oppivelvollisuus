package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class StudentInput(
    val valpasLink: String,
    val ssn: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate?,
    val phone: String,
    val email: String,
    val address: String
)

fun Handle.insertStudent(
    id: UUID,
    data: StudentInput
) {
    createUpdate(
        """
INSERT INTO students (id, valpas_link, ssn, first_name, last_name, date_of_birth, phone, email, address) 
VALUES (:id, :valpasLink, :ssn, :firstName, :lastName, :dateOfBirth, :phone, :email, :address)
"""
    )
        .bind("id", id)
        .bindKotlin(data)
        .execute()
}

data class StudentSummary(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val openedAt: LocalDate?
)

fun Handle.getStudentSummaries(): List<StudentSummary> = createQuery(
"""
SELECT id, first_name, last_name, sc.opened_at
FROM students
LEFT JOIN LATERAL (
    SELECT opened_at
    FROM student_cases
    WHERE student_id = students.id
    ORDER BY opened_at DESC
    LIMIT 1
) sc ON true
ORDER BY opened_at DESC, first_name, last_name
"""
)
    .mapTo<StudentSummary>()
    .list()

data class Student(
    val id: UUID,
    val valpasLink: String,
    val ssn: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate?,
    val phone: String,
    val email: String,
    val address: String
)

fun Handle.getStudent(id: UUID) = createQuery(
"""
SELECT id, valpas_link, ssn, first_name, last_name, date_of_birth, phone, email, address
FROM students
WHERE id = :id
"""
)
    .bind("id", id)
    .mapTo<Student>()
    .findOne()
    .getOrNull()
    ?: error("not found")

fun Handle.updateStudent(id: UUID, data: StudentInput) {
    createUpdate(
"""
UPDATE students 
SET 
    valpas_link = :valpasLink,
    ssn = :ssn,
    first_name = :firstName,
    last_name = :lastName,
    date_of_birth = :dateOfBirth,
    phone = :phone,
    email = :email,
    address = :address
WHERE id = :id
"""
    )
        .bind("id", id)
        .bindKotlin(data)
        .execute()
        .also { if (it != 1) error("not found") }
}
