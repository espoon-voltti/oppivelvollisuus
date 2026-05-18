// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.dev

import fi.espoo.oppivelvollisuus.CaseEventId
import fi.espoo.oppivelvollisuus.EspooUserId
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.domain.CaseBackgroundReason
import fi.espoo.oppivelvollisuus.domain.CaseEventType
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.domain.CaseStatus
import fi.espoo.oppivelvollisuus.domain.Gender
import fi.espoo.oppivelvollisuus.domain.NotInSchoolReason
import fi.espoo.oppivelvollisuus.domain.OtherNotifier
import fi.espoo.oppivelvollisuus.domain.PartnerOrganisation
import fi.espoo.oppivelvollisuus.domain.SchoolBackground
import fi.espoo.oppivelvollisuus.domain.ValpasNotifier
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.springframework.core.io.ClassPathResource

private val logger = KotlinLogging.logger {}

fun Database.Transaction.runDevScript(devScriptName: String) {
    val path = "dev-data/$devScriptName"
    logger.info { "Running SQL script: $path" }
    ClassPathResource(path).inputStream.use {
        it.bufferedReader().readText().let { content -> execute { sql(content) } }
    }
}

fun Database.Transaction.resetDatabase() {
    execute { sql("SELECT reset_database()") }
}

private val defaultDevTime: HelsinkiDateTime =
    HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))

data class DevUser(
    val id: EspooUserId = EspooUserId(UUID.randomUUID()),
    val externalId: String = "test-${UUID.randomUUID()}",
    val firstNames: String = "Teija",
    val lastName: String = "Testaaja",
    val email: String? = null,
    val isActive: Boolean = true,
    val created: HelsinkiDateTime = defaultDevTime,
) {
    val user: AuthenticatedUser.EspooUser
        get() = AuthenticatedUser.EspooUser(id)

    val name: String
        get() = "$firstNames $lastName"
}

fun Database.Transaction.insert(row: DevUser): EspooUserId =
    createUpdate {
            sql(
                """
                INSERT INTO users (id, external_id, first_names, last_name, email, is_active, created, updated)
                VALUES (${bind(row.id)}, ${bind(row.externalId)}, ${bind(row.firstNames)}, ${bind(row.lastName)}, ${bind(row.email)}, ${bind(row.isActive)}, ${bind(row.created)}, ${bind(row.created)})
                RETURNING id
                """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<EspooUserId>()

data class DevStudent(
    val id: StudentId = StudentId(UUID.randomUUID()),
    val createdBy: EspooUserId,
    val created: HelsinkiDateTime = defaultDevTime,
    val valpasLink: String = "",
    val ssn: String = "",
    val firstName: String = "Testi",
    val lastName: String = "Testilä",
    val language: String = "",
    val dateOfBirth: LocalDate = LocalDate.of(2010, 1, 1),
    val phone: String = "",
    val email: String = "",
    val gender: Gender? = null,
    val address: String = "",
    val municipalityInFinland: Boolean = true,
    val guardianInfo: String = "",
    val supportContactsInfo: String = "",
    val partnerOrganisations: Set<PartnerOrganisation> = emptySet(),
)

fun Database.Transaction.insert(row: DevStudent): StudentId =
    createUpdate {
            sql(
                """
                INSERT INTO students (id, created, created_by, valpas_link, ssn, first_name, last_name, language, date_of_birth, phone, email, gender, address, municipality_in_finland, guardian_info, support_contacts_info, partner_organisations)
                VALUES (${bind(row.id)}, ${bind(row.created)}, ${bind(row.createdBy)}, ${bind(row.valpasLink)}, ${bind(row.ssn)}, ${bind(row.firstName)}, ${bind(row.lastName)}, ${bind(row.language)}, ${bind(row.dateOfBirth)}, ${bind(row.phone)}, ${bind(row.email)}, ${bind(row.gender)}, ${bind(row.address)}, ${bind(row.municipalityInFinland)}, ${bind(row.guardianInfo)}, ${bind(row.supportContactsInfo)}, ${bind(row.partnerOrganisations.toTypedArray())})
                RETURNING id
                """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StudentId>()

data class DevStudentCase(
    val id: StudentCaseId = StudentCaseId(UUID.randomUUID()),
    val studentId: StudentId,
    val createdBy: EspooUserId,
    val created: HelsinkiDateTime = defaultDevTime,
    val openedAt: LocalDate = LocalDate.of(2026, 1, 1),
    val assignedTo: EspooUserId? = null,
    val status: CaseStatus = CaseStatus.TODO,
    val source: CaseSource = CaseSource.VALPAS_AUTOMATIC_CHECK,
    val sourceValpas: ValpasNotifier? = null,
    val sourceOther: OtherNotifier? = null,
    val sourceContact: String = "",
    val schoolBackground: Set<SchoolBackground> = emptySet(),
    val caseBackgroundReasons: Set<CaseBackgroundReason> = emptySet(),
    val notInSchoolReason: NotInSchoolReason? = null,
)

fun Database.Transaction.insert(row: DevStudentCase): StudentCaseId =
    createUpdate {
            sql(
                """
                INSERT INTO student_cases (id, created, created_by, student_id, opened_at, assigned_to, status, source, source_valpas, source_other, source_contact, school_background, case_background_reasons, not_in_school_reason)
                VALUES (${bind(row.id)}, ${bind(row.created)}, ${bind(row.createdBy)}, ${bind(row.studentId)}, ${bind(row.openedAt)}, ${bind(row.assignedTo)}, ${bind(row.status)}, ${bind(row.source)}, ${bind(row.sourceValpas)}, ${bind(row.sourceOther)}, ${bind(row.sourceContact)}, ${bind(row.schoolBackground.toTypedArray())}, ${bind(row.caseBackgroundReasons.toTypedArray())}, ${bind(row.notInSchoolReason)})
                RETURNING id
                """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StudentCaseId>()

data class DevCaseEvent(
    val id: CaseEventId = CaseEventId(UUID.randomUUID()),
    val studentCaseId: StudentCaseId,
    val createdBy: EspooUserId,
    val created: HelsinkiDateTime = defaultDevTime,
    val date: LocalDate = LocalDate.of(2026, 1, 1),
    val type: CaseEventType = CaseEventType.NOTE,
    val notes: String = "",
)

fun Database.Transaction.insert(row: DevCaseEvent): CaseEventId =
    createUpdate {
            sql(
                """
                INSERT INTO case_events (id, created, created_by, student_case_id, date, type, notes)
                VALUES (${bind(row.id)}, ${bind(row.created)}, ${bind(row.createdBy)}, ${bind(row.studentCaseId)}, ${bind(row.date)}, ${bind(row.type)}, ${bind(row.notes)})
                RETURNING id
                """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<CaseEventId>()
