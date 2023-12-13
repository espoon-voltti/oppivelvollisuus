package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.common.BadRequest
import fi.espoo.oppivelvollisuus.common.NotFound
import fi.espoo.oppivelvollisuus.common.UserBasics
import fi.espoo.oppivelvollisuus.config.AuthenticatedUser
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import java.time.LocalDate
import java.util.*

enum class CaseStatus {
    TODO,
    ON_HOLD,
    FINISHED
}

enum class CaseSource {
    VALPAS_NOTICE,
    VALPAS_AUTOMATIC_CHECK,
    OTHER
}

enum class ValpasNotifier {
    PERUSOPETUS,
    AIKUISTEN_PERUSOPETUS,
    AMMATILLINEN_PERUSTUTKINTO,
    LUKIO,
    AIKUISLUKIO,
    YLEISOPPILAITOKSEN_TUVA,
    AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO,
    AMMATILLISEN_ERITYISOPPILAITOKSEN_TUVA,
    TELMA,
    TOINEN_ASUINKUNTA
}

enum class OtherNotifier {
    ENNAKOIVA_OHJAUS,
    TYOLLISYYSPALVELUT,
    OMA_YHTEYDENOTTO,
    OHJAAMOTALO,
    OPPILAITOS,
    LASTENSUOJELU,
    OTHER
}

data class StudentCaseInput(
    val openedAt: LocalDate,
    val assignedTo: UUID?,
    val source: CaseSource,
    val sourceValpas: ValpasNotifier?,
    val sourceOther: OtherNotifier?,
    val sourceContact: String
) {
    init {
        if ((source == CaseSource.VALPAS_NOTICE) != (sourceValpas != null)) {
            throw BadRequest("sourceValpas must be present if and only if source is VALPAS_NOTICE")
        }
        if ((source == CaseSource.OTHER) != (sourceOther != null)) {
            throw BadRequest("sourceOther must be present if and only if source is OTHER")
        }
    }
}

fun Handle.insertStudentCase(
    studentId: UUID,
    data: StudentCaseInput,
    user: AuthenticatedUser
): UUID {
    return createUpdate(
        """
                INSERT INTO student_cases (created_by, student_id, opened_at, assigned_to, status, source, source_valpas, source_other, source_contact) 
                VALUES (:user, :studentId, :openedAt, :assignedTo, 'TODO', :source, :sourceValpas, :sourceOther, :sourceContact)
                RETURNING id
            """
    )
        .bind("studentId", studentId)
        .bindKotlin(data)
        .bind("user", user.id)
        .executeAndReturnGeneratedKeys()
        .mapTo<UUID>()
        .one()
}

enum class CaseFinishedReason {
    BEGAN_STUDIES,
    COMPULSORY_EDUCATION_ENDED,
    COMPULSORY_EDUCATION_SUSPENDED,
    COMPULSORY_EDUCATION_SUSPENDED_PERMANENTLY,
    MOVED_TO_ANOTHER_MUNICIPALITY,
    MOVED_ABROAD,
    ERRONEOUS_NOTICE,
    OTHER
}

enum class SchoolType {
    PERUSOPETUKSEEN_VALMISTAVA,
    AIKUISTEN_PERUSOPETUS,
    AMMATILLINEN_PERUSTUTKINTO,
    LUKIO,
    AIKUISLUKIO,
    YLEISOPPILAITOKSEN_TUVA,
    AMMATILLISEN_OPPILAITOKSEN_TUVA,
    AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO,
    TELMA,
    KANSANOPISTO,
    OTHER
}

data class FinishedInfo(
    @PropagateNull val reason: CaseFinishedReason,
    val startedAtSchool: SchoolType?
) {
    init {
        if ((reason == CaseFinishedReason.BEGAN_STUDIES) != (startedAtSchool != null)) {
            throw BadRequest("startedAtSchool must be present if and only if finished reason is BEGAN_STUDIES")
        }
    }
}

data class StudentCase(
    val id: UUID,
    val studentId: UUID,
    val openedAt: LocalDate,
    @Nested("assignedTo") val assignedTo: UserBasics?,
    val status: CaseStatus,
    @Nested("finishedInfo") val finishedInfo: FinishedInfo?,
    val source: CaseSource,
    val sourceValpas: ValpasNotifier?,
    val sourceOther: OtherNotifier?,
    val sourceContact: String
) {
    init {
        if ((status == CaseStatus.FINISHED) != (finishedInfo != null)) {
            throw BadRequest("finishedInfo must be present if and only if status is FINISHED")
        }
        if ((source == CaseSource.VALPAS_NOTICE) != (sourceValpas != null)) {
            throw BadRequest("sourceValpas must be present if and only if source is VALPAS_NOTICE")
        }
        if ((source == CaseSource.OTHER) != (sourceOther != null)) {
            throw BadRequest("sourceOther must be present if and only if source is OTHER")
        }
    }
}

fun Handle.getStudentCasesByStudent(studentId: UUID): List<StudentCase> = createQuery(
"""
SELECT 
    sc.id, sc.student_id, sc.opened_at, 
    assignee.id AS assigned_to_id, 
    assignee.first_name || ' ' || assignee.last_name AS assigned_to_name,
    sc.status,
    sc.finished_reason AS finished_info_reason,
    sc.started_at_school AS finished_info_started_at_school,
    sc.source,
    sc.source_valpas,
    sc.source_other,
    sc.source_contact
FROM student_cases sc
LEFT JOIN users assignee ON sc.assigned_to = assignee.id
WHERE student_id = :studentId
ORDER BY opened_at DESC, sc.created DESC 
"""
)
    .bind("studentId", studentId)
    .mapTo<StudentCase>()
    .list()

fun Handle.updateStudentCase(id: UUID, studentId: UUID, data: StudentCaseInput, user: AuthenticatedUser) {
    createUpdate(
"""
UPDATE student_cases
SET 
    updated = now(),
    updated_by = :user,
    opened_at = :openedAt,
    assigned_to = :assignedTo,
    source = :source,
    source_valpas = :sourceValpas,
    source_other = :sourceOther,
    source_contact = :sourceContact
WHERE id = :id AND student_id = :studentId
"""
    )
        .bind("id", id)
        .bind("studentId", studentId)
        .bindKotlin(data)
        .bind("user", user.id)
        .execute()
        .also { if (it != 1) throw NotFound() }
}

data class CaseStatusInput(
    val status: CaseStatus,
    val finishedInfo: FinishedInfo?
) {
    init {
        if ((status == CaseStatus.FINISHED) != (finishedInfo != null)) {
            throw BadRequest("finishedInfo must be present if and only if status is FINISHED")
        }
    }
}

fun Handle.updateStudentCaseStatus(id: UUID, studentId: UUID, data: CaseStatusInput, user: AuthenticatedUser) {
    createUpdate(
        """
UPDATE student_cases
SET 
    updated = now(),
    updated_by = :user,
    status = :status,
    finished_reason = :finishedReason,
    started_at_school = :startedAtSchool
WHERE id = :id AND student_id = :studentId
"""
    )
        .bind("id", id)
        .bind("studentId", studentId)
        .bind("status", data.status)
        .bind("finishedReason", data.finishedInfo?.reason)
        .bind("startedAtSchool", data.finishedInfo?.startedAtSchool)
        .bind("user", user.id)
        .execute()
        .also { if (it != 1) throw NotFound() }
}
