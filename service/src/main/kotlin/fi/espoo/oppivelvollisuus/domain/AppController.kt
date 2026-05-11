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
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class AppController {
    data class StudentAndCaseInput(
        val student: StudentInput,
        val studentCase: StudentCaseInput
    )

    @PostMapping("/students")
    fun createStudent(
        user: AuthenticatedUser,
        @RequestBody body: StudentAndCaseInput,
        db: Database
    ): StudentId =
        db
            .connect {
                it.transaction { tx ->
                    val studentId = tx.insertStudent(data = body.student, user = user)
                    tx.insertStudentCase(studentId = studentId, data = body.studentCase, user = user)
                    studentId
                }
            }.also { Audit.CREATE_STUDENT.log(targetId = AuditId(it)) }

    @PostMapping("/students/duplicates")
    fun getDuplicateStudents(
        user: AuthenticatedUser,
        @RequestBody body: DuplicateStudentCheckInput,
        db: Database
    ): List<DuplicateStudent> =
        db
            .connect {
                it.read { tx ->
                    tx.getPossibleDuplicateStudents(body)
                }
            }.also { Audit.GET_DUPLICATE_STUDENTS.log() }

    @PostMapping("/students/search")
    fun getStudents(
        user: AuthenticatedUser,
        @RequestBody body: StudentSearchParams,
        db: Database
    ): List<StudentSummary> =
        db
            .connect {
                it.read { tx ->
                    tx.getStudentSummaries(
                        params =
                            body.copy(
                                query = body.query.takeIf { !it.isNullOrBlank() }
                            )
                    )
                }
            }.also { Audit.SEARCH_STUDENTS.log() }

    data class StudentResponse(
        val student: Student,
        val cases: List<StudentCase>
    )

    @GetMapping("/students/{id}")
    fun getStudent(
        user: AuthenticatedUser,
        @PathVariable id: StudentId,
        db: Database
    ): StudentResponse =
        db
            .connect {
                it.read { tx ->
                    val studentDetails = tx.getStudent(id = id)
                    val cases = tx.getStudentCasesByStudent(studentId = id)
                    StudentResponse(studentDetails, cases)
                }
            }.also { Audit.GET_STUDENT.log(targetId = AuditId(id)) }

    @PutMapping("/students/{id}")
    fun updateStudent(
        user: AuthenticatedUser,
        @PathVariable id: StudentId,
        @RequestBody body: StudentInput,
        db: Database
    ) {
        db
            .connect {
                it.transaction { tx ->
                    tx.updateStudent(id = id, data = body, user = user)
                }
            }.also { Audit.UPDATE_STUDENT.log(targetId = AuditId(id)) }
    }

    @DeleteMapping("/students/{id}")
    fun deleteStudent(
        user: AuthenticatedUser,
        @PathVariable id: StudentId,
        db: Database
    ) {
        db
            .connect {
                it.transaction { tx ->
                    tx.deleteStudent(id = id)
                }
            }.also { Audit.DELETE_STUDENT.log(targetId = AuditId(id)) }
    }

    @PostMapping("/students/{studentId}/cases")
    fun createStudentCase(
        user: AuthenticatedUser,
        @PathVariable studentId: StudentId,
        @RequestBody body: StudentCaseInput,
        db: Database
    ): StudentCaseId =
        db
            .connect {
                it.transaction { tx ->
                    tx.insertStudentCase(studentId = studentId, data = body, user = user)
                }
            }.also { Audit.CREATE_STUDENT_CASE.log(targetId = AuditId(it), objectId = AuditId(studentId)) }

    @PutMapping("/students/{studentId}/cases/{id}")
    fun updateStudentCase(
        user: AuthenticatedUser,
        @PathVariable studentId: StudentId,
        @PathVariable id: StudentCaseId,
        @RequestBody body: StudentCaseInput,
        db: Database
    ) {
        db
            .connect {
                it.transaction { tx ->
                    tx.updateStudentCase(id = id, studentId = studentId, data = body, user = user)
                }
            }.also { Audit.UPDATE_STUDENT_CASE.log(targetId = AuditId(id), objectId = AuditId(studentId)) }
    }

    @DeleteMapping("/students/{studentId}/cases/{id}")
    fun deleteStudentCase(
        user: AuthenticatedUser,
        @PathVariable studentId: StudentId,
        @PathVariable id: StudentCaseId,
        db: Database
    ) {
        db
            .connect {
                it.transaction { tx ->
                    tx.deleteStudentCase(id = id, studentId = studentId)
                }
            }.also { Audit.DELETE_STUDENT_CASE.log(targetId = AuditId(id), objectId = AuditId(studentId)) }
    }

    @PutMapping("/students/{studentId}/cases/{id}/status")
    fun updateStudentCaseStatus(
        user: AuthenticatedUser,
        @PathVariable studentId: StudentId,
        @PathVariable id: StudentCaseId,
        @RequestBody body: CaseStatusInput,
        db: Database
    ) {
        db
            .connect {
                it.transaction { tx ->
                    tx.updateStudentCaseStatus(id = id, studentId = studentId, data = body, user = user)
                }
            }.also { Audit.UPDATE_STUDENT_CASE_STATUS.log(targetId = AuditId(id), objectId = AuditId(studentId)) }
    }

    @PostMapping("/student-cases/{studentCaseId}/case-events")
    fun createCaseEvent(
        user: AuthenticatedUser,
        @PathVariable studentCaseId: StudentCaseId,
        @RequestBody body: CaseEventInput,
        db: Database
    ): CaseEventId =
        db
            .connect {
                it.transaction { tx ->
                    tx.insertCaseEvent(studentCaseId = studentCaseId, data = body, user = user)
                }
            }.also { Audit.CREATE_CASE_EVENT.log(targetId = AuditId(it), objectId = AuditId(studentCaseId)) }

    @PutMapping("/case-events/{id}")
    fun updateCaseEvent(
        user: AuthenticatedUser,
        @PathVariable id: CaseEventId,
        @RequestBody body: CaseEventInput,
        db: Database
    ) {
        db
            .connect {
                it.transaction { tx ->
                    tx.updateCaseEvent(id = id, data = body, user = user)
                }
            }.also { Audit.UPDATE_CASE_EVENT.log(targetId = AuditId(id)) }
    }

    @DeleteMapping("/case-events/{id}")
    fun deleteCaseEvent(
        user: AuthenticatedUser,
        @PathVariable id: CaseEventId,
        db: Database
    ) {
        db
            .connect {
                it.transaction { tx ->
                    tx.deleteCaseEvent(id = id)
                }
            }.also { Audit.DELETE_CASE_EVENT.log(targetId = AuditId(id)) }
    }

    @GetMapping("/employees")
    fun getEmployeeUsers(
        user: AuthenticatedUser,
        db: Database
    ): List<AppUser> = db.connect { it.read { tx -> tx.getActiveAppUsers() } }.also { Audit.GET_EMPLOYEES.log() }

    @GetMapping("/reports/student-cases")
    fun getCasesReport(
        user: AuthenticatedUser,
        @RequestParam(required = false) start: LocalDate?,
        @RequestParam(required = false) end: LocalDate?,
        db: Database
    ): List<CaseReportRow> =
        db.connect { it.read { tx -> tx.getCasesReport(CaseReportRequest(start, end)) } }.also { Audit.GET_CASES_REPORT.log() }

    @DeleteMapping("/old-students")
    fun deleteOldStudents(
        user: AuthenticatedUser,
        db: Database
    ) = db.connect { it.transaction { tx -> tx.deleteOldStudents() } }.also { Audit.DELETE_OLD_STUDENTS.log() }
}
