// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.EspooUserId
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.UserBasics
import fi.espoo.oppivelvollisuus.shared.BadRequest
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.db.DatabaseEnum
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json

enum class CaseStatus : DatabaseEnum {
    IMPORTED_FROM_VALPAS,
    TODO,
    ON_HOLD,
    FINISHED;

    override val sqlType: String = "case_status"
}

enum class CaseSource : DatabaseEnum {
    VALPAS_NOTICE,
    VALPAS_AUTOMATIC_CHECK,
    OTHER;

    override val sqlType: String = "case_source"
}

enum class ValpasNotifier : DatabaseEnum {
    PERUSOPETUS,
    AIKUISTEN_PERUSOPETUS,
    AMMATILLINEN_PERUSTUTKINTO,
    LUKIO,
    AIKUISLUKIO,
    YLEISOPPILAITOKSEN_TUVA,
    AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO,
    AMMATILLISEN_ERITYISOPPILAITOKSEN_TUVA,
    TELMA,
    TOINEN_ASUINKUNTA,
    OPISTO;

    override val sqlType: String = "valpas_notifier"
}

enum class OtherNotifier : DatabaseEnum {
    ENNAKOIVA_OHJAUS,
    TYOLLISYYSPALVELUT,
    OMA_YHTEYDENOTTO,
    OHJAAMOTALO,
    OPPILAITOS,
    LASTENSUOJELU,
    OTHER;

    override val sqlType: String = "other_notifier"
}

enum class SchoolBackground : DatabaseEnum {
    PERUSKOULUN_PAATTOTODISTUS,
    EI_PERUSKOULUN_PAATTOTODISTUSTA,
    KESKEYTYNEET_TOISEN_ASTEEN_OPINNOT,
    KESKEYTYNEET_NIVELVAIHEEN_OPINNOT,
    VSOP_PERUSKOULUSSA,
    YLEINEN_TUKI_PERUSKOULUSSA,
    TEHOSTETTU_TUKI_PERUSKOULUSSA,
    TEHOSTETTU_HENKKOHT_TUKI_PERUSKOULUSSA,
    ERITYISEN_TUEN_PAATOS_PERUSKOULUSSA,
    YKSILOLLISTETTY_OPPIMAARA_AIDINKIELESSA_JA_MATEMATIIKASSA,
    PERUSOPETUKSEEN_VALMISTAVA_OPISKELU_SUOMESSA,
    ULKOMAILLA_SUORITETUT_PERUSOPETUSTA_VASTAAVAT_OPINNOT;

    override val sqlType: String = "school_background"
}

enum class CaseBackgroundReason : DatabaseEnum {
    MOTIVAATION_PUUTE,
    VAARA_ALAVALINTA,
    OPPIMISVAIKEUDET,
    ELAMANHALLINNAN_HAASTEET,
    POISSAOLOT,
    TERVEYDELLISET_PERUSTEET,
    MUUTTO_PAIKKAKUNNALLE,
    MUUTTO_ULKOMAILLE,
    MAAHAN_MUUTTANUT_NUORI_ILMAN_OPISKELUPAIKKAA,
    JAANYT_ILMAN_OPISKELUPAIKKAA,
    MUU_SYY;

    override val sqlType: String = "case_background_reason"
}

enum class NotInSchoolReason : DatabaseEnum {
    KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA,
    EI_OLE_VASTAANOTTANUT_SAAMAANSA_OPISKELUPAIKKAA,
    EI_OLE_ALOITTANUT_VASTAANOTTAMASSAAN_OPISKELUPAIKASSA,
    EI_OLE_HAKEUTUNUT_JATKO_OPINTOIHIN,
    EI_OPISKELUPAIKKAA_YLEISOPPILAITOKSESSA,
    EI_OPISKELUPAIKKAA_AMMATILLISESSA_ERITYISOPPILAITOKSESSA,
    EI_OLE_SAANUT_OPISKELUPAIKKAA_KIELITAIDON_VUOKSI,
    OPINNOT_ULKOMAILLA,
    MUU_SYY;

    override val sqlType: String = "not_in_school_reason"
}

data class StudentCaseInput(
    val openedAt: LocalDate,
    val assignedTo: EspooUserId?,
    val source: CaseSource,
    val sourceValpas: ValpasNotifier?,
    val sourceOther: OtherNotifier?,
    val sourceContact: String,
    val schoolBackground: Set<SchoolBackground>,
    val caseBackgroundReasons: Set<CaseBackgroundReason>,
    val notInSchoolReason: NotInSchoolReason?,
) {
    init {
        if ((source == CaseSource.OTHER) != (sourceOther != null)) {
            throw BadRequest("sourceOther must be present if and only if source is OTHER")
        }
    }
}

fun Database.Transaction.insertStudentCase(
    studentId: StudentId,
    data: StudentCaseInput,
    createdBy: EspooUserId,
    now: HelsinkiDateTime,
): StudentCaseId =
    createUpdate {
            sql(
                """
                INSERT INTO student_cases (created, created_by, student_id, opened_at, assigned_to, status, source, source_valpas, source_other, source_contact, school_background, case_background_reasons, not_in_school_reason)
                VALUES (${bind(now)}, ${bind(createdBy)}, ${bind(studentId)}, ${bind(data.openedAt)}, ${bind(data.assignedTo)}, ${bind(CaseStatus.TODO)}, ${bind(data.source)}, ${bind(data.sourceValpas)}, ${bind(data.sourceOther)}, ${bind(data.sourceContact)}, ${bind(data.schoolBackground.toTypedArray())}, ${bind(data.caseBackgroundReasons.toTypedArray())}, ${bind(data.notInSchoolReason)})
                RETURNING id
                """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StudentCaseId>()

enum class CaseFinishedReason : DatabaseEnum {
    BEGAN_STUDIES,
    COMPULSORY_EDUCATION_ENDED,
    COMPULSORY_EDUCATION_SUSPENDED,
    COMPULSORY_EDUCATION_SUSPENDED_PERMANENTLY,
    MOVED_TO_ANOTHER_MUNICIPALITY,
    MOVED_ABROAD,
    ERRONEOUS_NOTICE,
    OTHER;

    override val sqlType: String = "case_finished_reason"
}

enum class SchoolType : DatabaseEnum {
    PERUSOPETUKSEEN_VALMISTAVA,
    AIKUISTEN_PERUSOPETUS,
    AIKUISTEN_PERUSOPETUS_SUOMEEN_MUUTTANEILLE,
    AMMATILLINEN_PERUSTUTKINTO,
    LUKIO,
    AIKUISLUKIO,
    YLEISOPPILAITOKSEN_TUVA,
    AMMATILLISEN_OPPILAITOKSEN_TUVA,
    AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO,
    TELMA,
    KANSANOPISTO,
    OTHER;

    override val sqlType: String = "school_type"
}

enum class FollowUpMeasure : DatabaseEnum {
    KELA_REHABILITATION_SERVICES,
    SOCIAL_SERVICES,
    YOUTH_WORK,
    JOB_SEARCH_SUPPORT,
    LANGUAGE_COURSE,
    MISSING,
    MOVE_ABROAD;

    override val sqlType: String = "follow_up_measure"
}

data class FinishedInfo(
    @param:PropagateNull val reason: CaseFinishedReason,
    val startedAtSchool: SchoolType?,
    val followUpMeasures: Set<FollowUpMeasure>?,
    val otherReason: String?,
) {
    init {
        if ((reason == CaseFinishedReason.BEGAN_STUDIES) != (startedAtSchool != null)) {
            throw BadRequest(
                "startedAtSchool must be present if and only if finished reason is BEGAN_STUDIES"
            )
        }
        if (
            (reason == CaseFinishedReason.COMPULSORY_EDUCATION_ENDED) != (followUpMeasures != null)
        ) {
            throw BadRequest(
                "followUpMeasure must be present if and only if finished reason is COMPULSORY_EDUCATION_ENDED"
            )
        }
    }
}

data class StudentCase(
    val id: StudentCaseId,
    val studentId: StudentId,
    val openedAt: LocalDate,
    @param:Nested("assignedTo") val assignedTo: UserBasics?,
    val status: CaseStatus,
    @param:Nested("finishedInfo") val finishedInfo: FinishedInfo?,
    val source: CaseSource,
    val sourceValpas: ValpasNotifier?,
    val sourceOther: OtherNotifier?,
    val sourceContact: String,
    val schoolBackground: Set<SchoolBackground>,
    val caseBackgroundReasons: Set<CaseBackgroundReason>,
    val notInSchoolReason: NotInSchoolReason?,
    val valpasNotificationId: UUID?,
    @param:Json val events: List<CaseEvent>,
) {
    init {
        if ((status == CaseStatus.FINISHED) != (finishedInfo != null)) {
            throw BadRequest("finishedInfo must be present if and only if status is FINISHED")
        }
        if (status != CaseStatus.IMPORTED_FROM_VALPAS) {
            if ((source == CaseSource.VALPAS_NOTICE) != (sourceValpas != null)) {
                throw BadRequest(
                    "sourceValpas must be present if and only if source is VALPAS_NOTICE"
                )
            }
        }
        if ((source == CaseSource.OTHER) != (sourceOther != null)) {
            throw BadRequest("sourceOther must be present if and only if source is OTHER")
        }
    }
}

fun Database.Read.getStudentCasesByStudent(studentId: StudentId): List<StudentCase> =
    createQuery {
            sql(
                """
                SELECT
                    sc.id, sc.student_id, sc.opened_at,
                    assignee.id AS assigned_to_id,
                    assignee.first_name || ' ' || assignee.last_name AS assigned_to_name,
                    sc.status,
                    sc.finished_reason AS finished_info_reason,
                    sc.other_reason AS finished_info_other_reason,
                    sc.started_at_school AS finished_info_started_at_school,
                    sc.follow_up_measures AS finished_info_follow_up_measures,
                    sc.source,
                    sc.source_valpas,
                    sc.source_other,
                    sc.source_contact,
                    sc.school_background,
                    sc.case_background_reasons,
                    sc.not_in_school_reason,
                    sc.valpas_notification_id,
                    coalesce((
                        SELECT jsonb_agg(jsonb_build_object(
                            'id', e.id,
                            'studentCaseId', e.student_case_id,
                            'date', e.date,
                            'type', e.type,
                            'notes', e.notes,
                            'created', jsonb_build_object(
                                'name', creator.first_name || ' ' || creator.last_name,
                                'time', e.created
                            ),
                            'updated', (CASE WHEN updater.id IS NOT NULL THEN jsonb_build_object(
                                'name', updater.first_name || ' ' || updater.last_name,
                                'time', e.updated
                            ) END)
                        ) ORDER BY date DESC, e.created DESC)
                        FROM case_events e
                        JOIN users creator ON e.created_by = creator.id
                        LEFT JOIN users updater ON e.updated_by = updater.id
                        WHERE student_case_id = sc.id
                    ), '[]'::jsonb) AS events
                FROM student_cases sc
                LEFT JOIN users assignee ON sc.assigned_to = assignee.id
                WHERE student_id = ${bind(studentId)}
                """
            )
        }
        .toList<StudentCase>()

fun Database.Transaction.updateStudentCase(
    id: StudentCaseId,
    studentId: StudentId,
    data: StudentCaseInput,
    updatedBy: EspooUserId,
    now: HelsinkiDateTime,
) {
    createUpdate {
            sql(
                """
                UPDATE student_cases
                SET
                    updated = ${bind(now)},
                    updated_by = ${bind(updatedBy)},
                    opened_at = ${bind(data.openedAt)},
                    assigned_to = ${bind(data.assignedTo)},
                    source = ${bind(data.source)},
                    source_valpas = ${bind(data.sourceValpas)},
                    source_other = ${bind(data.sourceOther)},
                    source_contact = ${bind(data.sourceContact)},
                    school_background = ${bind(data.schoolBackground.toTypedArray())},
                    case_background_reasons = ${bind(data.caseBackgroundReasons.toTypedArray())},
                    not_in_school_reason = ${bind(data.notInSchoolReason)}
                WHERE id = ${bind(id)} AND student_id = ${bind(studentId)}
                """
            )
        }
        .updateExactlyOne()
}

data class CaseStatusInput(val status: CaseStatus, val finishedInfo: FinishedInfo?) {
    init {
        if ((status == CaseStatus.FINISHED) != (finishedInfo != null)) {
            throw BadRequest("finishedInfo must be present if and only if status is FINISHED")
        }
    }
}

fun Database.Transaction.updateStudentCaseStatus(
    id: StudentCaseId,
    studentId: StudentId,
    data: CaseStatusInput,
    updatedBy: EspooUserId,
    now: HelsinkiDateTime,
) {
    createUpdate {
            sql(
                """
                UPDATE student_cases
                SET
                    updated = ${bind(now)},
                    updated_by = ${bind(updatedBy)},
                    status = ${bind(data.status)},
                    finished_reason = ${bind(data.finishedInfo?.reason)},
                    started_at_school = ${bind(data.finishedInfo?.startedAtSchool)},
                    follow_up_measures = ${bind(data.finishedInfo?.followUpMeasures?.toTypedArray())},
                    other_reason = ${bind(data.finishedInfo?.otherReason)}
                WHERE id = ${bind(id)} AND student_id = ${bind(studentId)}
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.deleteStudentCase(id: StudentCaseId, studentId: StudentId) {
    createUpdate {
            sql(
                "DELETE FROM student_cases WHERE id = ${bind(id)} AND student_id = ${bind(studentId)}"
            )
        }
        .updateExactlyOne()
}

fun Database.Read.findImportedFromValpasCaseForStudent(studentId: StudentId): StudentCaseId? =
    createQuery {
            sql(
                """
                SELECT id FROM student_cases
                WHERE student_id = ${bind(studentId)}
                  AND status = ${bind(CaseStatus.IMPORTED_FROM_VALPAS)}
                """
            )
        }
        .exactlyOneOrNull<StudentCaseId>()

fun Database.Transaction.insertImportedCase(
    studentId: StudentId,
    valpasNotificationId: UUID,
    openedAt: LocalDate,
    now: HelsinkiDateTime,
): StudentCaseId =
    createUpdate {
            sql(
                """
                INSERT INTO student_cases (
                    created, created_by, student_id, opened_at, assigned_to,
                    status, source, source_valpas, source_other, source_contact,
                    school_background, case_background_reasons, not_in_school_reason,
                    valpas_notification_id
                ) VALUES (
                    ${bind(now)},
                    ${bind(AuthenticatedUser.SystemInternalUser.espooUserId)},
                    ${bind(studentId)}, ${bind(openedAt)}, NULL,
                    ${bind(CaseStatus.IMPORTED_FROM_VALPAS)},
                    ${bind(CaseSource.VALPAS_NOTICE)},
                    NULL, NULL, '',
                    ${bind(emptyArray<SchoolBackground>())},
                    ${bind(emptyArray<CaseBackgroundReason>())},
                    NULL,
                    ${bind(valpasNotificationId)}
                )
                RETURNING id
                """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StudentCaseId>()

fun Database.Read.existsCaseWithNotificationId(notificationId: UUID): Boolean =
    createQuery {
            sql(
                """
                SELECT EXISTS (
                    SELECT 1 FROM student_cases
                    WHERE valpas_notification_id = ${bind(notificationId)}
                )
                """
            )
        }
        .exactlyOne<Boolean>()

fun Database.Read.findNewValpasNotificationIds(ids: Set<UUID>): Set<UUID> {
    if (ids.isEmpty()) return emptySet()
    return createQuery {
            sql(
                """
                SELECT incoming.id
                FROM unnest(${bind(ids.toTypedArray())}) AS incoming(id)
                WHERE NOT EXISTS (
                    SELECT 1 FROM student_cases sc
                    WHERE sc.valpas_notification_id = incoming.id
                )
                """
            )
        }
        .toList<UUID>()
        .toSet()
}

fun Database.Read.getStudentCaseStatus(id: StudentCaseId): CaseStatus? =
    createQuery { sql("SELECT status FROM student_cases WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<CaseStatus>()

fun Database.Read.findStudentIdByCaseId(id: StudentCaseId): StudentId? =
    createQuery { sql("SELECT student_id FROM student_cases WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<StudentId>()

fun Database.Read.studentHasActiveCase(studentId: StudentId): Boolean =
    createQuery {
            sql(
                """
                SELECT EXISTS (
                    SELECT 1 FROM student_cases
                    WHERE student_id = ${bind(studentId)}
                      AND status IN (${bind(CaseStatus.TODO)}, ${bind(CaseStatus.ON_HOLD)})
                )
                """
            )
        }
        .exactlyOne<Boolean>()

fun Database.Transaction.copyValpasNotificationIdAndDeleteSource(
    sourceId: StudentCaseId,
    targetId: StudentCaseId,
    notificationId: UUID,
    updatedBy: EspooUserId,
    now: HelsinkiDateTime,
) {
    // Delete the source first to release the unique constraint on valpas_notification_id,
    // then update the target — otherwise the unique constraint fires on the UPDATE.
    createUpdate { sql("DELETE FROM student_cases WHERE id = ${bind(sourceId)}") }
        .updateExactlyOne()
    createUpdate {
            sql(
                """
                UPDATE student_cases
                SET valpas_notification_id = ${bind(notificationId)},
                    updated = ${bind(now)},
                    updated_by = ${bind(updatedBy)}
                WHERE id = ${bind(targetId)}
                """
            )
        }
        .updateExactlyOne()
}
