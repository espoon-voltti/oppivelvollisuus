// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.EspooUserId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.UserBasics
import fi.espoo.oppivelvollisuus.shared.NotFound
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.db.DatabaseEnum
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import java.time.LocalDate
import kotlin.math.max
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull

enum class Gender : DatabaseEnum {
    MALE,
    FEMALE,
    OTHER;

    override val sqlType: String = "gender"
}

enum class PartnerOrganisation : DatabaseEnum {
    LASTENSUOJELU,
    TERVEYDENHUOLTO,
    MIELENTERVEYSPALVELUT,
    TUKIHENKILO,
    TYOPAJATOIMINTA,
    KOLMAS_SEKTORI;

    override val sqlType: String = "partner_organisation"
}

data class StudentInput(
    val valpasLink: String,
    val valpasOppijaOid: String?,
    val ssn: String,
    val firstName: String,
    val lastName: String,
    val language: String,
    val dateOfBirth: LocalDate,
    val phone: String,
    val email: String,
    val gender: Gender?,
    val address: String,
    val municipalityInFinland: Boolean,
    val guardianInfo: String,
    val supportContactsInfo: String,
    val partnerOrganisations: Set<PartnerOrganisation> = emptySet(),
)

fun Database.Transaction.insertStudent(
    data: StudentInput,
    createdBy: EspooUserId,
    now: HelsinkiDateTime,
): StudentId =
    createUpdate {
            sql(
                """
                INSERT INTO students (created, created_by, valpas_link, valpas_oppija_oid, ssn, first_name, last_name, language, date_of_birth, phone, email, gender, address, municipality_in_finland, guardian_info, support_contacts_info, partner_organisations)
                VALUES (${bind(now)}, ${bind(createdBy)}, ${bind(data.valpasLink)}, ${bind(data.valpasOppijaOid)}, ${bind(data.ssn)}, ${bind(data.firstName)}, ${bind(data.lastName)}, ${bind(data.language)}, ${bind(data.dateOfBirth)}, ${bind(data.phone)}, ${bind(data.email)}, ${bind(data.gender)}, ${bind(data.address)}, ${bind(data.municipalityInFinland)}, ${bind(data.guardianInfo)}, ${bind(data.supportContactsInfo)}, ${bind(data.partnerOrganisations.toTypedArray())})
                RETURNING id
                """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StudentId>()

data class StudentSummary(
    val id: StudentId,
    val firstName: String,
    val lastName: String,
    val openedAt: LocalDate?,
    val status: CaseStatus?,
    val source: CaseSource?,
    @param:Nested("assignedTo") val assignedTo: UserBasics?,
    @param:Nested("event") val lastEvent: CaseEventSummary?,
)

data class CaseEventSummary(
    @param:PropagateNull val date: LocalDate,
    val type: CaseEventType,
    val notes: String,
)

data class AssignedToSearch(
    // null = not assigned
    val assignedTo: EspooUserId?
)

data class StudentSearchParams(
    val query: String?,
    val statuses: List<CaseStatus>,
    val sources: List<CaseSource>,
    val assignee: AssignedToSearch?,
)

fun Database.Read.getStudentSummaries(params: StudentSearchParams): List<StudentSummary> =
    createQuery {
            sql(
                """
                SELECT s.id, s.first_name, s.last_name, sc.opened_at, sc.status, sc.source,
                    assignee.id AS assigned_to_id,
                    assignee.first_name || ' ' || assignee.last_name AS assigned_to_name,
                    ce.date AS event_date, ce.type AS event_type, ce.notes AS event_notes
                FROM students s
                LEFT JOIN LATERAL (
                    SELECT id, opened_at, assigned_to, status, source
                    FROM student_cases
                    WHERE student_id = s.id
                    ORDER BY
                      CASE status
                        WHEN 'IMPORTED_FROM_VALPAS' THEN 0
                        WHEN 'TODO'                 THEN 1
                        WHEN 'ON_HOLD'              THEN 1
                        WHEN 'FINISHED'             THEN 2
                      END,
                      opened_at DESC NULLS LAST
                    LIMIT 1
                ) sc ON true
                LEFT JOIN LATERAL (
                    SELECT date, type, notes
                    FROM case_events
                    WHERE student_case_id = sc.id
                    ORDER BY date DESC, created DESC
                    LIMIT 1
                ) ce ON true
                LEFT JOIN users assignee ON sc.assigned_to = assignee.id
                WHERE (status IS NULL OR status = ANY(${bind(params.statuses.toTypedArray())}))
                  AND (source IS NULL OR source = ANY(${bind(params.sources.toTypedArray())}))
                ${if (params.assignee == null) {
                    ""
                } else if (params.assignee.assignedTo == null) {
                    "AND assignee.id IS NULL"
                } else {
                    "AND assignee.id = ${bind(params.assignee.assignedTo)}"
                }}
                ${if (params.query != null) {
                    """
                    AND (EXISTS (
                        SELECT 1
                        FROM unnest(regexp_split_to_array(lower(s.first_name), '\s+')) AS t(name)
                        WHERE name LIKE ${bind(params.query.trim().lowercase())} || '%'
                            OR lower(name || ' ' || s.last_name) LIKE ${bind(params.query.trim().lowercase())} || '%'
                            OR lower(s.last_name || ' ' || name) LIKE ${bind(params.query.trim().lowercase())} || '%'
                      ) OR
                        lower(s.last_name) LIKE ${bind(params.query.trim().lowercase())} || '%' OR
                        lower(s.first_name || ' ' || s.last_name) LIKE ${bind(params.query.trim().lowercase())} || '%' OR
                        lower(s.last_name || ' ' || s.first_name) LIKE ${bind(params.query.trim().lowercase())} || '%' OR
                        lower(s.ssn) LIKE ${bind(params.query.trim().lowercase())} || '%')
                    """
                } else {
                    ""
                }}
                ORDER BY opened_at DESC NULLS LAST, last_name, first_name
                """
            )
        }
        .toList<StudentSummary>()
        .map {
            it.copy(
                lastEvent =
                    it.lastEvent?.copy(
                        notes =
                            if (it.lastEvent.notes.length > 100) {
                                it.lastEvent.notes.substring(0, 100).let { str ->
                                    val lastSpace = max(str.lastIndexOf(' '), 50)
                                    str.substring(0, lastSpace) + "..."
                                }
                            } else {
                                it.lastEvent.notes
                            }
                    )
            )
        }

data class Student(
    val id: StudentId,
    val valpasLink: String,
    val valpasOppijaOid: String?,
    val ssn: String,
    val firstName: String,
    val lastName: String,
    val language: String,
    val dateOfBirth: LocalDate,
    val phone: String,
    val email: String,
    val gender: Gender?,
    val address: String,
    val municipalityInFinland: Boolean,
    val guardianInfo: String,
    val supportContactsInfo: String,
    val partnerOrganisations: Set<PartnerOrganisation> = emptySet(),
)

fun Database.Read.getStudent(id: StudentId): Student =
    createQuery {
            sql(
                """
                SELECT id, valpas_link, valpas_oppija_oid, ssn, first_name, last_name, language, date_of_birth, phone, email, gender, address, municipality_in_finland, guardian_info, support_contacts_info, partner_organisations
                FROM students
                WHERE id = ${bind(id)}
                """
            )
        }
        .exactlyOneOrNull<Student>() ?: throw NotFound()

fun Database.Transaction.updateStudent(
    id: StudentId,
    data: StudentInput,
    updatedBy: EspooUserId,
    now: HelsinkiDateTime,
) {
    createUpdate {
            sql(
                """
                UPDATE students
                SET
                    updated = ${bind(now)},
                    updated_by = ${bind(updatedBy)},
                    valpas_link = ${bind(data.valpasLink)},
                    valpas_oppija_oid = ${bind(data.valpasOppijaOid)},
                    ssn = ${bind(data.ssn)},
                    first_name = ${bind(data.firstName)},
                    last_name = ${bind(data.lastName)},
                    language = ${bind(data.language)},
                    date_of_birth = ${bind(data.dateOfBirth)},
                    phone = ${bind(data.phone)},
                    email = ${bind(data.email)},
                    gender = ${bind(data.gender)},
                    address = ${bind(data.address)},
                    municipality_in_finland = ${bind(data.municipalityInFinland)},
                    guardian_info = ${bind(data.guardianInfo)},
                    support_contacts_info = ${bind(data.supportContactsInfo)},
                    partner_organisations = ${bind(data.partnerOrganisations.toTypedArray())}
                WHERE id = ${bind(id)}
                """
            )
        }
        .updateExactlyOne()
}

data class DuplicateStudentCheckInput(
    val ssn: String,
    val valpasLink: String,
    val firstName: String,
    val lastName: String,
)

data class DuplicateStudent(
    val id: StudentId,
    val name: String,
    val dateOfBirth: LocalDate,
    val matchingSsn: Boolean,
    val matchingValpasLink: Boolean,
    val matchingName: Boolean,
)

fun Database.Read.getPossibleDuplicateStudents(
    input: DuplicateStudentCheckInput
): List<DuplicateStudent> =
    createQuery {
            sql(
                """
                WITH match_data AS (
                    SELECT
                        id,
                        last_name || ' ' || first_name AS name,
                        date_of_birth,
                        ${if (input.ssn.isNotBlank()) "(lower(ssn) = lower(${bind(input.ssn)}))" else "FALSE"} AS matching_ssn,
                        ${if (input.valpasLink.isNotBlank()) "(lower(valpas_link) = lower(${bind(input.valpasLink)}))" else "FALSE"} AS matching_valpas_link,
                        ${if (input.firstName.isNotBlank() && input.lastName.isNotBlank()) {
                    """(
                            lower(first_name) = lower(${bind(input.firstName)}) AND
                            lower(last_name) = lower(${bind(input.lastName)}) AND
                            (ssn = '' OR ${bind(input.ssn)} = '')
                        )"""
                } else {
                    "FALSE"
                }} AS matching_name
                    FROM students
                )
                SELECT * FROM match_data
                WHERE matching_ssn OR matching_valpas_link OR matching_name
                """
            )
        }
        .toList<DuplicateStudent>()

fun Database.Transaction.deleteStudent(id: StudentId) {
    createUpdate { sql("DELETE FROM students WHERE id = ${bind(id)}") }.updateExactlyOne()
}

fun Database.Transaction.deleteOldStudents(thresholdDate: LocalDate) {
    createUpdate {
            sql(
                """
                WITH students_to_delete AS (
                    SELECT id
                    FROM students
                    WHERE date_of_birth < ${bind(thresholdDate)}
                    FOR UPDATE SKIP LOCKED
                ), cases_to_delete AS (
                    SELECT sc.id
                    FROM student_cases sc
                    JOIN students_to_delete s ON s.id = sc.student_id
                    FOR UPDATE SKIP LOCKED
                ), deleted_events AS (
                    DELETE FROM case_events
                    WHERE student_case_id IN (SELECT id FROM cases_to_delete)
                ), deleted_cases AS (
                    DELETE FROM student_cases
                    WHERE id IN (SELECT id FROM cases_to_delete)
                )
                DELETE FROM students
                WHERE id IN (SELECT id FROM students_to_delete)
                """
            )
        }
        .execute()
}
