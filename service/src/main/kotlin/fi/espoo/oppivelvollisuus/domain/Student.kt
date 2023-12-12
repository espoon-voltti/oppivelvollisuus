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
    val address: String,
    val guardianInfo: String,
    val supportContactsInfo: String
)

fun Handle.insertStudent(
    data: StudentInput,
    user: AuthenticatedUser
): UUID {
    return createUpdate(
        """
INSERT INTO students (created_by, valpas_link, ssn, first_name, last_name, date_of_birth, phone, email, address, guardian_info, support_contacts_info) 
VALUES (:user, :valpasLink, :ssn, :firstName, :lastName, :dateOfBirth, :phone, :email, :address, :guardianInfo, :supportContactsInfo)
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
    val status: CaseStatus?,
    @Nested("assignedTo") val assignedTo: UserBasics?
)

data class StudentSearchParams(
    val query: String?,
    val statuses: List<CaseStatus>,
    val assignedTo: UUID?
)

fun Handle.getStudentSummaries(params: StudentSearchParams): List<StudentSummary> = createQuery(
"""
SELECT s.id, s.first_name, s.last_name, sc.opened_at, sc.status,
    assignee.id AS assigned_to_id, 
    assignee.first_name || ' ' || assignee.last_name AS assigned_to_name
FROM students s
LEFT JOIN LATERAL (
    SELECT opened_at, assigned_to, status
    FROM student_cases
    WHERE student_id = s.id
    ORDER BY status != 'FINISHED' DESC, opened_at DESC
    LIMIT 1
) sc ON true
LEFT JOIN users assignee ON sc.assigned_to = assignee.id
WHERE (status IS NULL OR status = ANY(:statuses::case_status[]))
${if (params.assignedTo != null) "AND assignee.id = :assignedTo" else ""}
${if (params.query != null) {
        """
    AND (lower(s.first_name) LIKE :query || '%' OR 
        lower(s.last_name) LIKE :query || '%' OR 
        lower(s.first_name || ' ' || s.last_name) LIKE :query || '%' OR
        lower(s.last_name || ' ' || s.first_name) LIKE :query || '%' OR
        lower(s.ssn) LIKE :query || '%')
"""
    } else {
        ""
    }}
ORDER BY opened_at DESC NULLS LAST, last_name, first_name
"""
)
    .bindKotlin(params.copy(query = params.query?.trim()?.lowercase()))
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
    val address: String,
    val guardianInfo: String,
    val supportContactsInfo: String
)

fun Handle.getStudent(id: UUID) = createQuery(
"""
SELECT id, valpas_link, ssn, first_name, last_name, date_of_birth, phone, email, address, guardian_info, support_contacts_info
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
    address = :address,
    guardian_info = :guardianInfo,
    support_contacts_info = :supportContactsInfo
WHERE id = :id
"""
    )
        .bind("id", id)
        .bindKotlin(data)
        .bind("user", user.id)
        .execute()
        .also { if (it != 1) throw NotFound() }
}
