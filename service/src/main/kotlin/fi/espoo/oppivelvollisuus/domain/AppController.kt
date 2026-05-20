// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.AppUser
import fi.espoo.oppivelvollisuus.CaseEventId
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.getActiveAppUsers
import fi.espoo.oppivelvollisuus.shared.Audit
import fi.espoo.oppivelvollisuus.shared.AuditId
import fi.espoo.oppivelvollisuus.shared.BadRequest
import fi.espoo.oppivelvollisuus.shared.Conflict
import fi.espoo.oppivelvollisuus.shared.NotFound
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.AppClock
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/espoo-user")
class AppController {
    data class StudentAndCaseInput(val student: StudentInput, val studentCase: StudentCaseInput)

    @PostMapping("/students")
    fun createStudent(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        clock: AppClock,
        @RequestBody body: StudentAndCaseInput,
    ): StudentId =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val now = clock.now()
                    val studentId =
                        tx.insertStudent(data = body.student, createdBy = user.id, now = now)
                    tx.insertStudentCase(
                        studentId = studentId,
                        data = body.studentCase,
                        createdBy = user.id,
                        now = now,
                    )
                    studentId
                }
            }
            .also { Audit.CreateStudent.log(targetId = AuditId(it)) }

    @PostMapping("/students/duplicates")
    fun getDuplicateStudents(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        @RequestBody body: DuplicateStudentCheckInput,
    ): List<DuplicateStudent> =
        db.connect { dbc -> dbc.read { tx -> tx.getPossibleDuplicateStudents(body) } }
            .also { Audit.GetDuplicateStudents.log() }

    @PostMapping("/students/search")
    fun getStudents(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        @RequestBody body: StudentSearchParams,
    ): List<StudentSummary> =
        db.connect { dbc ->
                dbc.read { tx ->
                    tx.getStudentSummaries(
                        params = body.copy(query = body.query.takeIf { !it.isNullOrBlank() })
                    )
                }
            }
            .also { Audit.SearchStudents.log() }

    data class StudentResponse(val student: Student, val cases: List<StudentCase>)

    @GetMapping("/students/{id}")
    fun getStudent(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        @PathVariable id: StudentId,
    ): StudentResponse =
        db.connect { dbc ->
                dbc.read { tx ->
                    val studentDetails = tx.getStudent(id = id)
                    val cases = tx.getStudentCasesByStudent(studentId = id)
                    StudentResponse(studentDetails, cases)
                }
            }
            .also { Audit.GetStudent.log(targetId = AuditId(id)) }

    @PutMapping("/students/{id}")
    fun updateStudent(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        clock: AppClock,
        @PathVariable id: StudentId,
        @RequestBody body: StudentInput,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    tx.updateStudent(id = id, data = body, updatedBy = user.id, now = clock.now())
                }
            }
            .also { Audit.UpdateStudent.log(targetId = AuditId(id)) }
    }

    @DeleteMapping("/students/{id}")
    fun deleteStudent(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        @PathVariable id: StudentId,
    ) {
        db.connect { dbc -> dbc.transaction { tx -> tx.deleteStudent(id = id) } }
            .also { Audit.DeleteStudent.log(targetId = AuditId(id)) }
    }

    @PostMapping("/students/{studentId}/cases")
    fun createStudentCase(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        clock: AppClock,
        @PathVariable studentId: StudentId,
        @RequestBody body: StudentCaseInput,
    ): StudentCaseId =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    tx.insertStudentCase(
                        studentId = studentId,
                        data = body,
                        createdBy = user.id,
                        now = clock.now(),
                    )
                }
            }
            .also {
                Audit.CreateStudentCase.log(targetId = AuditId(studentId), objectId = AuditId(it))
            }

    @PutMapping("/students/{studentId}/cases/{id}")
    fun updateStudentCase(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        clock: AppClock,
        @PathVariable studentId: StudentId,
        @PathVariable id: StudentCaseId,
        @RequestBody body: StudentCaseInput,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    tx.updateStudentCase(
                        id = id,
                        studentId = studentId,
                        data = body,
                        updatedBy = user.id,
                        now = clock.now(),
                    )
                }
            }
            .also {
                Audit.UpdateStudentCase.log(targetId = AuditId(studentId), objectId = AuditId(id))
            }
    }

    @DeleteMapping("/students/{studentId}/cases/{id}")
    fun deleteStudentCase(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        @PathVariable studentId: StudentId,
        @PathVariable id: StudentCaseId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val caseStatus = tx.getStudentCaseStatus(id) ?: throw NotFound()
                    if (caseStatus == CaseStatus.IMPORTED_FROM_VALPAS) {
                        throw BadRequest("Operation not allowed on IMPORTED_FROM_VALPAS case")
                    }
                    tx.deleteStudentCase(id = id, studentId = studentId)
                }
            }
            .also {
                Audit.DeleteStudentCase.log(targetId = AuditId(studentId), objectId = AuditId(id))
            }
    }

    @PostMapping("/student-cases/{caseId}/mark-as-duplicate-of-latest")
    fun markAsDuplicateOfLatest(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        clock: AppClock,
        @PathVariable caseId: StudentCaseId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val studentId = tx.findStudentIdByCaseId(caseId) ?: throw NotFound()
                    val cases = tx.getStudentCasesByStudent(studentId)
                    val imported = cases.firstOrNull() ?: throw NotFound()
                    check(imported.id == caseId) {
                        "expected imported case $caseId to be first in cases list"
                    }
                    if (imported.status != CaseStatus.IMPORTED_FROM_VALPAS) {
                        throw BadRequest("Case is not in IMPORTED_FROM_VALPAS status")
                    }
                    val target =
                        cases.getOrNull(1) ?: throw BadRequest("No other case to merge into")
                    if (target.valpasNotificationId != null) {
                        throw Conflict("Target case already has a valpas_notification_id")
                    }
                    val notificationId =
                        requireNotNull(imported.valpasNotificationId) {
                            "imported case missing valpas_notification_id"
                        }
                    tx.copyValpasNotificationIdAndDeleteSource(
                        sourceId = imported.id,
                        targetId = target.id,
                        notificationId = notificationId,
                        updatedBy = user.id,
                        now = clock.now(),
                    )
                }
            }
            .also { Audit.MarkCaseAsDuplicateOfLatest.log(targetId = AuditId(caseId)) }
    }

    @PutMapping("/students/{studentId}/cases/{id}/status")
    fun updateStudentCaseStatus(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        clock: AppClock,
        @PathVariable studentId: StudentId,
        @PathVariable id: StudentCaseId,
        @RequestBody body: CaseStatusInput,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val before =
                        tx.getStudentCasesByStudent(studentId).find { it.id == id }
                            ?: throw NotFound()
                    if (before.status == CaseStatus.IMPORTED_FROM_VALPAS) {
                        if (body.status != CaseStatus.TODO) {
                            throw BadRequest(
                                "From IMPORTED_FROM_VALPAS only target=TODO is allowed"
                            )
                        }
                        if (
                            before.source == CaseSource.VALPAS_NOTICE && before.sourceValpas == null
                        ) {
                            throw BadRequest("sourceValpas must be set before approval")
                        }
                        if (tx.studentHasNonFinishedCaseOtherThan(studentId, id)) {
                            throw Conflict("Student has another non-finished case")
                        }
                    } else if (body.status == CaseStatus.IMPORTED_FROM_VALPAS) {
                        throw BadRequest(
                            "Cannot transition into IMPORTED_FROM_VALPAS via this endpoint"
                        )
                    }
                    tx.updateStudentCaseStatus(
                        id = id,
                        studentId = studentId,
                        data = body,
                        updatedBy = user.id,
                        now = clock.now(),
                    )
                }
            }
            .also {
                Audit.UpdateStudentCaseStatus.log(
                    targetId = AuditId(studentId),
                    objectId = AuditId(id),
                )
            }
    }

    @PostMapping("/student-cases/{studentCaseId}/case-events")
    fun createCaseEvent(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        clock: AppClock,
        @PathVariable studentCaseId: StudentCaseId,
        @RequestBody body: CaseEventInput,
    ): CaseEventId =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val caseStatus = tx.getStudentCaseStatus(studentCaseId) ?: throw NotFound()
                    if (caseStatus == CaseStatus.IMPORTED_FROM_VALPAS) {
                        throw BadRequest("Operation not allowed on IMPORTED_FROM_VALPAS case")
                    }
                    tx.insertCaseEvent(
                        studentCaseId = studentCaseId,
                        data = body,
                        createdBy = user.id,
                        now = clock.now(),
                    )
                }
            }
            .also {
                Audit.CreateCaseEvent.log(targetId = AuditId(studentCaseId), objectId = AuditId(it))
            }

    @PutMapping("/case-events/{id}")
    fun updateCaseEvent(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        clock: AppClock,
        @PathVariable id: CaseEventId,
        @RequestBody body: CaseEventInput,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val studentCaseId = tx.getStudentCaseIdByEventId(id) ?: throw NotFound()
                    val caseStatus = tx.getStudentCaseStatus(studentCaseId) ?: throw NotFound()
                    if (caseStatus == CaseStatus.IMPORTED_FROM_VALPAS) {
                        throw BadRequest("Operation not allowed on IMPORTED_FROM_VALPAS case")
                    }
                    tx.updateCaseEvent(id = id, data = body, updatedBy = user.id, now = clock.now())
                }
            }
            .also { Audit.UpdateCaseEvent.log(targetId = AuditId(id)) }
    }

    @DeleteMapping("/case-events/{id}")
    fun deleteCaseEvent(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        @PathVariable id: CaseEventId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val studentCaseId = tx.getStudentCaseIdByEventId(id) ?: throw NotFound()
                    val caseStatus = tx.getStudentCaseStatus(studentCaseId) ?: throw NotFound()
                    if (caseStatus == CaseStatus.IMPORTED_FROM_VALPAS) {
                        throw BadRequest("Operation not allowed on IMPORTED_FROM_VALPAS case")
                    }
                    tx.deleteCaseEvent(id = id)
                }
            }
            .also { Audit.DeleteCaseEvent.log(targetId = AuditId(id)) }
    }

    @GetMapping("/employees")
    fun getEmployeeUsers(db: Database, user: AuthenticatedUser.EspooUser): List<AppUser> =
        db.connect { dbc -> dbc.read { it.getActiveAppUsers() } }.also { Audit.GetEmployees.log() }

    @GetMapping("/reports/student-cases")
    fun getCasesReport(
        db: Database,
        user: AuthenticatedUser.EspooUser,
        @RequestParam(required = false) start: LocalDate?,
        @RequestParam(required = false) end: LocalDate?,
    ): List<CaseReportRow> =
        db.connect { dbc -> dbc.read { it.getCasesReport(CaseReportRequest(start, end)) } }
            .also { Audit.GetCasesReport.log() }

    @DeleteMapping("/old-students")
    fun deleteOldStudents(db: Database, user: AuthenticatedUser.EspooUser, clock: AppClock) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    tx.deleteOldStudents(thresholdDate = clock.today().minusYears(21))
                }
            }
            .also { Audit.DeleteOldStudents.log() }
    }
}
