// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.dev

import fi.espoo.oppivelvollisuus.AppUser
import fi.espoo.oppivelvollisuus.CaseEventId
import fi.espoo.oppivelvollisuus.EspooUserId
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.domain.CaseBackgroundReason
import fi.espoo.oppivelvollisuus.domain.CaseEventType
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.domain.Gender
import fi.espoo.oppivelvollisuus.domain.NotInSchoolReason
import fi.espoo.oppivelvollisuus.domain.OtherNotifier
import fi.espoo.oppivelvollisuus.domain.SchoolBackground
import fi.espoo.oppivelvollisuus.domain.ValpasNotifier
import fi.espoo.oppivelvollisuus.shared.db.Database
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun Database.Transaction.runDevScript(devScriptName: String) {
    val path = "dev-data/$devScriptName"
    logger.info { "Running SQL script: $path" }
    val content = ClassPathResource(path).inputStream.bufferedReader().use { it.readText() }
    handle.createScript(content).execute()
}

private val CREATE_RESET_FUNCTION =
    """
    CREATE OR REPLACE FUNCTION reset_database() RETURNS void AS ${'$'}${'$'}
    BEGIN
      EXECUTE (
        SELECT 'TRUNCATE TABLE ' || string_agg(quote_ident(table_name), ', ') || ' CASCADE'
        FROM information_schema.tables
        WHERE table_schema = 'public'
        AND table_type = 'BASE TABLE'
        AND table_name <> 'flyway_schema_history'
      );
      IF (SELECT count(*) FROM information_schema.sequences) > 0 THEN
        EXECUTE (
          SELECT 'SELECT ' || string_agg(format('setval(%L, %L, false)', sequence_name, start_value), ', ')
          FROM information_schema.sequences
          WHERE sequence_schema = 'public'
        );
      END IF;
    END ${'$'}${'$'} LANGUAGE plpgsql;
    """.trimIndent()

fun Database.Transaction.resetDatabase() {
    handle.createScript(CREATE_RESET_FUNCTION).execute()
    handle.createScript("SELECT reset_database()").execute()
}

data class DevAppUser(
    val id: EspooUserId = EspooUserId(UUID.randomUUID()),
    val externalId: String,
    val firstName: String,
    val lastName: String,
    val email: String?
)

fun Database.Transaction.devInsert(appUser: DevAppUser): AppUser =
    handle
        .createQuery(
            """
            INSERT INTO users (id, external_id, first_names, last_name, email, is_active)
            VALUES (:id, :externalId, :firstName, :lastName, :email, true)
            ON CONFLICT (external_id) DO UPDATE
            SET updated = now(), first_names = :firstName, last_name = :lastName, email = :email
            RETURNING id, external_id, first_name, last_name, email, is_active
            """.trimIndent()
        ).bind("id", appUser.id.raw)
        .bind("externalId", appUser.externalId)
        .bind("firstName", appUser.firstName)
        .bind("lastName", appUser.lastName)
        .bind("email", appUser.email)
        .mapTo(AppUser::class.java)
        .one()

data class DevStudent(
    val id: StudentId = StudentId(UUID.randomUUID()),
    val valpasLink: String = "",
    val ssn: String = "",
    val firstName: String,
    val lastName: String,
    val language: String = "",
    val dateOfBirth: LocalDate,
    val phone: String = "",
    val email: String = "",
    val gender: Gender? = null,
    val address: String = "",
    val municipalityInFinland: Boolean = true,
    val guardianInfo: String = "",
    val supportContactsInfo: String = "",
    val createdBy: EspooUserId? = null
)

fun Database.Transaction.devInsert(student: DevStudent): StudentId =
    handle
        .createUpdate(
            """
            INSERT INTO students (id, valpas_link, ssn, first_name, last_name, language, date_of_birth, phone, email, gender, address, municipality_in_finland, guardian_info, support_contacts_info, created_by)
            VALUES (:id, :valpasLink, :ssn, :firstName, :lastName, :language, :dateOfBirth, :phone, :email, :gender, :address, :municipalityInFinland, :guardianInfo, :supportContactsInfo, :createdBy)
            RETURNING id
            """.trimIndent()
        ).bind("id", student.id.raw)
        .bind("valpasLink", student.valpasLink)
        .bind("ssn", student.ssn)
        .bind("firstName", student.firstName)
        .bind("lastName", student.lastName)
        .bind("language", student.language)
        .bind("dateOfBirth", student.dateOfBirth)
        .bind("phone", student.phone)
        .bind("email", student.email)
        .bind("gender", student.gender?.name)
        .bind("address", student.address)
        .bind("municipalityInFinland", student.municipalityInFinland)
        .bind("guardianInfo", student.guardianInfo)
        .bind("supportContactsInfo", student.supportContactsInfo)
        .bind("createdBy", student.createdBy?.raw)
        .executeAndReturnGeneratedKeys()
        .mapTo(StudentId::class.java)
        .one()

data class DevStudentCase(
    val id: StudentCaseId = StudentCaseId(UUID.randomUUID()),
    val studentId: StudentId,
    val openedAt: LocalDate,
    val assignedTo: EspooUserId? = null,
    val source: CaseSource,
    val sourceValpas: ValpasNotifier? = null,
    val sourceOther: OtherNotifier? = null,
    val sourceContact: String = "",
    val schoolBackground: Set<SchoolBackground> = emptySet(),
    val caseBackgroundReasons: Set<CaseBackgroundReason> = emptySet(),
    val notInSchoolReason: NotInSchoolReason? = null,
    val createdBy: EspooUserId? = null
)

fun Database.Transaction.devInsert(studentCase: DevStudentCase): StudentCaseId =
    handle
        .createUpdate(
            """
            INSERT INTO student_cases (id, student_id, opened_at, assigned_to, status, source, source_valpas, source_other, source_contact, school_background, case_background_reasons, not_in_school_reason, created_by)
            VALUES (:id, :studentId, :openedAt, :assignedTo, 'TODO', :source, :sourceValpas, :sourceOther, :sourceContact, :schoolBackground::school_background[], :caseBackgroundReasons::case_background_reason[], :notInSchoolReason, :createdBy)
            RETURNING id
            """.trimIndent()
        ).bind("id", studentCase.id.raw)
        .bind("studentId", studentCase.studentId.raw)
        .bind("openedAt", studentCase.openedAt)
        .bind("assignedTo", studentCase.assignedTo?.raw)
        .bind("source", studentCase.source.name)
        .bind("sourceValpas", studentCase.sourceValpas?.name)
        .bind("sourceOther", studentCase.sourceOther?.name)
        .bind("sourceContact", studentCase.sourceContact)
        .bind("schoolBackground", studentCase.schoolBackground.map { it.name }.toTypedArray())
        .bind("caseBackgroundReasons", studentCase.caseBackgroundReasons.map { it.name }.toTypedArray())
        .bind("notInSchoolReason", studentCase.notInSchoolReason?.name)
        .bind("createdBy", studentCase.createdBy?.raw)
        .executeAndReturnGeneratedKeys()
        .mapTo(StudentCaseId::class.java)
        .one()

data class DevCaseEvent(
    val id: CaseEventId = CaseEventId(UUID.randomUUID()),
    val studentCaseId: StudentCaseId,
    val date: LocalDate,
    val type: CaseEventType,
    val notes: String = "",
    val createdBy: EspooUserId? = null
)

fun Database.Transaction.devInsert(caseEvent: DevCaseEvent): CaseEventId =
    handle
        .createUpdate(
            """
            INSERT INTO case_events (id, student_case_id, date, type, notes, created_by)
            VALUES (:id, :studentCaseId, :date, :type, :notes, :createdBy)
            RETURNING id
            """.trimIndent()
        ).bind("id", caseEvent.id.raw)
        .bind("studentCaseId", caseEvent.studentCaseId.raw)
        .bind("date", caseEvent.date)
        .bind("type", caseEvent.type.name)
        .bind("notes", caseEvent.notes)
        .bind("createdBy", caseEvent.createdBy?.raw)
        .executeAndReturnGeneratedKeys()
        .mapTo(CaseEventId::class.java)
        .one()
