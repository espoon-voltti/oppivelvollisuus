package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.common.NotFound
import fi.espoo.oppivelvollisuus.common.UserBasics
import fi.espoo.oppivelvollisuus.config.AuthenticatedUser
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.statement.SqlStatements
import java.time.LocalDate
import java.util.*
import kotlin.jvm.optionals.getOrNull

enum class Gender {
    MALE,
    FEMALE,
    OTHER
}

data class StudentInput(
    val valpasLink: String,
    val ssn: String,
    val firstName: String,
    val lastName: String,
    val language: String,
    val dateOfBirth: LocalDate?,
    val phone: String,
    val email: String,
    val gender: Gender?,
    val address: String,
    val municipalityInFinland: Boolean,
    val guardianInfo: String,
    val supportContactsInfo: String
)

fun Handle.insertStudent(
    data: StudentInput,
    user: AuthenticatedUser
): UUID {
    return createUpdate(
        """
INSERT INTO students (created_by, valpas_link, ssn, first_name, last_name, language, date_of_birth, phone, email, gender, address, municipality_in_finland, guardian_info, support_contacts_info) 
VALUES (:user, :valpasLink, :ssn, :firstName, :lastName, :language, :dateOfBirth, :phone, :email, :gender, :address, :municipalityInFinland, :guardianInfo, :supportContactsInfo)
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
    val language: String,
    val dateOfBirth: LocalDate?,
    val phone: String,
    val email: String,
    val gender: Gender?,
    val address: String,
    val municipalityInFinland: Boolean,
    val guardianInfo: String,
    val supportContactsInfo: String
)

fun Handle.getStudent(id: UUID) = createQuery(
"""
SELECT id, valpas_link, ssn, first_name, last_name, language, date_of_birth, phone, email, gender, address, municipality_in_finland, guardian_info, support_contacts_info
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
    language = :language,
    date_of_birth = :dateOfBirth,
    phone = :phone,
    email = :email,
    gender = :gender,
    address = :address,
    municipality_in_finland = :municipalityInFinland, 
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

data class DuplicateStudentCheckInput(
    val ssn: String,
    val valpasLink: String,
    val firstName: String,
    val lastName: String
)

data class DuplicateStudent(
    val id: UUID,
    val name: String,
    val dateOfBirth: LocalDate?,
    val matchingSsn: Boolean,
    val matchingValpasLink: Boolean,
    val matchingName: Boolean
)

fun Handle.getPossibleDuplicateStudents(input: DuplicateStudentCheckInput): List<DuplicateStudent> {
    val ssnPredicate = "(lower(ssn) = lower(:ssn))"
        .takeIf { input.ssn.isNotBlank() }

    val valpasLinkPredicate = "(lower(valpas_link) = lower(:valpasLink))"
        .takeIf { input.valpasLink.isNotBlank() }

    val namePredicate = """(
        lower(first_name) = lower(:firstName) AND 
        lower(last_name) = lower(:lastName) AND 
        (ssn = '' OR :ssn = '')
    )"""
        .takeIf { input.firstName.isNotBlank() && input.lastName.isNotBlank() }

    return createQuery(
        """
        WITH match_data AS (
            SELECT 
                id,
                last_name || ' ' || first_name AS name,
                date_of_birth,
                ${ssnPredicate ?: "FALSE"} AS matching_ssn,
                ${valpasLinkPredicate ?: "FALSE"} AS matching_valpas_link,
                ${namePredicate ?: "FALSE"} AS matching_name
            FROM students
        )
        SELECT * FROM match_data
        WHERE matching_ssn OR matching_valpas_link OR matching_name
    """
    )
        .configure(SqlStatements::class.java) { it.setUnusedBindingAllowed(true) }
        .bindKotlin(input)
        .mapTo<DuplicateStudent>()
        .list()
}

fun Handle.deleteStudent(id: UUID) {
    createUpdate("DELETE FROM students WHERE id = :id")
        .bind("id", id)
        .execute()
        .also { if (it != 1) throw NotFound() }
}
