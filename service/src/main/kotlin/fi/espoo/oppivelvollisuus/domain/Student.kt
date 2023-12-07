package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.common.NotFound
import fi.espoo.oppivelvollisuus.common.UserBasics
import fi.espoo.oppivelvollisuus.config.AuthenticatedUser
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
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
    data: StudentInput,
    user: AuthenticatedUser
): UUID {
    return createUpdate(
        """
INSERT INTO students (created_by, valpas_link, ssn, first_name, last_name, date_of_birth, phone, email, address) 
VALUES (:user, :valpasLink, :ssn, :firstName, :lastName, :dateOfBirth, :phone, :email, :address)
RETURNING id
"""
    )
        .bindKotlin(data)
        .bind("user", user.id)
        .executeAndReturnGeneratedKeys()
        .mapTo<UUID>()
        .one()
}

data class StudentSummary(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val openedAt: LocalDate?,
    @Nested("assignedTo") val assignedTo: UserBasics?
)

fun Handle.getStudentSummaries(): List<StudentSummary> = createQuery(
"""
SELECT s.id, s.first_name, s.last_name, sc.opened_at, 
    assignee.id AS assigned_to_id, 
    assignee.first_name || ' ' || assignee.last_name AS assigned_to_name
FROM students s
LEFT JOIN LATERAL (
    SELECT opened_at, assigned_to
    FROM student_cases
    WHERE student_id = s.id
    ORDER BY opened_at DESC
    LIMIT 1
) sc ON true
LEFT JOIN users assignee ON sc.assigned_to = assignee.id
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
    ?: throw NotFound()

fun Handle.updateStudent(id: UUID, data: StudentInput, user: AuthenticatedUser) {
    createUpdate(
"""
UPDATE students 
SET 
    updated = now(),
    updated_by = :user,
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
        .bind("user", user.id)
        .execute()
        .also { if (it != 1) throw NotFound() }
}
