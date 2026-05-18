// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.AppUser
import fi.espoo.oppivelvollisuus.getActiveAppUsers
import fi.espoo.oppivelvollisuus.shared.Audit
import fi.espoo.oppivelvollisuus.shared.AuditId
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import java.time.LocalDate
import java.util.UUID
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.springframework.beans.factory.annotation.Autowired
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
    @Autowired lateinit var jdbi: Jdbi

    data class StudentAndCaseInput(val student: StudentInput, val studentCase: StudentCaseInput)

    @PostMapping("/students")
    fun createStudent(
        user: AuthenticatedUser.EspooUser,
        @RequestBody body: StudentAndCaseInput,
    ): UUID =
        jdbi
            .inTransactionUnchecked { tx ->
                val studentId = tx.insertStudent(data = body.student, user = user)
                tx.insertStudentCase(studentId = studentId, data = body.studentCase, user = user)

                studentId
            }
            .also { Audit.CreateStudent.log(targetId = AuditId(it)) }

    @PostMapping("/students/duplicates")
    fun getDuplicateStudents(
        user: AuthenticatedUser.EspooUser,
        @RequestBody body: DuplicateStudentCheckInput,
    ): List<DuplicateStudent> =
        jdbi
            .inTransactionUnchecked { tx -> tx.getPossibleDuplicateStudents(body) }
            .also { Audit.GetDuplicateStudents.log() }

    @PostMapping("/students/search")
    fun getStudents(
        user: AuthenticatedUser.EspooUser,
        @RequestBody body: StudentSearchParams,
    ): List<StudentSummary> =
        jdbi
            .inTransactionUnchecked { tx ->
                tx.getStudentSummaries(
                    params = body.copy(query = body.query.takeIf { !it.isNullOrBlank() })
                )
            }
            .also { Audit.SearchStudents.log() }

    data class StudentResponse(val student: Student, val cases: List<StudentCase>)

    @GetMapping("/students/{id}")
    fun getStudent(user: AuthenticatedUser.EspooUser, @PathVariable id: UUID): StudentResponse =
        jdbi
            .inTransactionUnchecked { tx ->
                val studentDetails = tx.getStudent(id = id)
                val cases = tx.getStudentCasesByStudent(studentId = id)
                StudentResponse(studentDetails, cases)
            }
            .also { Audit.GetStudent.log(targetId = AuditId(id)) }

    @PutMapping("/students/{id}")
    fun updateStudent(
        user: AuthenticatedUser.EspooUser,
        @PathVariable id: UUID,
        @RequestBody body: StudentInput,
    ) {
        jdbi
            .inTransactionUnchecked { tx -> tx.updateStudent(id = id, data = body, user = user) }
            .also { Audit.UpdateStudent.log(targetId = AuditId(id)) }
    }

    @DeleteMapping("/students/{id}")
    fun deleteStudent(user: AuthenticatedUser.EspooUser, @PathVariable id: UUID) {
        jdbi
            .inTransactionUnchecked { tx -> tx.deleteStudent(id = id) }
            .also { Audit.DeleteStudent.log(targetId = AuditId(id)) }
    }

    @PostMapping("/students/{studentId}/cases")
    fun createStudentCase(
        user: AuthenticatedUser.EspooUser,
        @PathVariable studentId: UUID,
        @RequestBody body: StudentCaseInput,
    ): UUID =
        jdbi
            .inTransactionUnchecked { tx ->
                tx.insertStudentCase(studentId = studentId, data = body, user = user)
            }
            .also {
                Audit.CreateStudentCase.log(targetId = AuditId(studentId), objectId = AuditId(it))
            }

    @PutMapping("/students/{studentId}/cases/{id}")
    fun updateStudentCase(
        user: AuthenticatedUser.EspooUser,
        @PathVariable studentId: UUID,
        @PathVariable id: UUID,
        @RequestBody body: StudentCaseInput,
    ) {
        jdbi
            .inTransactionUnchecked { tx ->
                tx.updateStudentCase(id = id, studentId = studentId, data = body, user = user)
            }
            .also {
                Audit.UpdateStudentCase.log(targetId = AuditId(studentId), objectId = AuditId(id))
            }
    }

    @DeleteMapping("/students/{studentId}/cases/{id}")
    fun deleteStudentCase(
        user: AuthenticatedUser.EspooUser,
        @PathVariable studentId: UUID,
        @PathVariable id: UUID,
    ) {
        jdbi
            .inTransactionUnchecked { tx -> tx.deleteStudentCase(id = id, studentId = studentId) }
            .also {
                Audit.DeleteStudentCase.log(targetId = AuditId(studentId), objectId = AuditId(id))
            }
    }

    @PutMapping("/students/{studentId}/cases/{id}/status")
    fun updateStudentCaseStatus(
        user: AuthenticatedUser.EspooUser,
        @PathVariable studentId: UUID,
        @PathVariable id: UUID,
        @RequestBody body: CaseStatusInput,
    ) {
        jdbi
            .inTransactionUnchecked { tx ->
                tx.updateStudentCaseStatus(id = id, studentId = studentId, data = body, user = user)
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
        user: AuthenticatedUser.EspooUser,
        @PathVariable studentCaseId: UUID,
        @RequestBody body: CaseEventInput,
    ): UUID =
        jdbi
            .inTransactionUnchecked { tx ->
                tx.insertCaseEvent(studentCaseId = studentCaseId, data = body, user = user)
            }
            .also {
                Audit.CreateCaseEvent.log(targetId = AuditId(studentCaseId), objectId = AuditId(it))
            }

    @PutMapping("/case-events/{id}")
    fun updateCaseEvent(
        user: AuthenticatedUser.EspooUser,
        @PathVariable id: UUID,
        @RequestBody body: CaseEventInput,
    ) {
        jdbi
            .inTransactionUnchecked { tx -> tx.updateCaseEvent(id = id, data = body, user = user) }
            .also { Audit.UpdateCaseEvent.log(targetId = AuditId(id)) }
    }

    @DeleteMapping("/case-events/{id}")
    fun deleteCaseEvent(user: AuthenticatedUser.EspooUser, @PathVariable id: UUID) {
        jdbi
            .inTransactionUnchecked { tx -> tx.deleteCaseEvent(id = id) }
            .also { Audit.DeleteCaseEvent.log(targetId = AuditId(id)) }
    }

    @GetMapping("/employees")
    fun getEmployeeUsers(user: AuthenticatedUser.EspooUser): List<AppUser> =
        jdbi.inTransactionUnchecked { it.getActiveAppUsers() }.also { Audit.GetEmployees.log() }

    @GetMapping("/reports/student-cases")
    fun getCasesReport(
        user: AuthenticatedUser.EspooUser,
        @RequestParam(required = false) start: LocalDate?,
        @RequestParam(required = false) end: LocalDate?,
    ): List<CaseReportRow> =
        jdbi
            .inTransactionUnchecked { it.getCasesReport(CaseReportRequest(start, end)) }
            .also { Audit.GetCasesReport.log() }

    @DeleteMapping("/old-students")
    fun deleteOldStudents(user: AuthenticatedUser.EspooUser) =
        jdbi
            .inTransactionUnchecked { it.deleteOldStudents() }
            .also { Audit.DeleteOldStudents.log() }
}
