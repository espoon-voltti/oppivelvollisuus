// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.CaseEventId
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.config.audit
import fi.espoo.oppivelvollisuus.shared.auth.AuthenticatedUser
import fi.espoo.oppivelvollisuus.shared.db.Database
import io.opentelemetry.api.trace.Tracer
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.beans.factory.annotation.Autowired
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
    @Autowired
    lateinit var jdbi: Jdbi

    @Autowired
    lateinit var tracer: Tracer

    private val logger = KotlinLogging.logger {}

    private fun db() = Database(jdbi, tracer)

    data class StudentAndCaseInput(
        val student: StudentInput,
        val studentCase: StudentCaseInput
    )

    @PostMapping("/students")
    fun createStudent(
        user: AuthenticatedUser,
        @RequestBody body: StudentAndCaseInput
    ): StudentId =
        db()
            .connect { it.transaction { tx ->
                val studentId = tx.insertStudent(data = body.student, user = user)
                tx.insertStudentCase(studentId = studentId, data = body.studentCase, user = user)
                studentId
            } }.also {
                logger.audit(
                    user,
                    "CREATE_STUDENT"
                )
            }

    @PostMapping("/students/duplicates")
    fun getDuplicateStudents(
        user: AuthenticatedUser,
        @RequestBody body: DuplicateStudentCheckInput
    ): List<DuplicateStudent> =
        db()
            .connect { it.read { tx ->
                tx.getPossibleDuplicateStudents(body)
            } }.also {
                logger.audit(
                    user,
                    "GET_DUPLICATE_STUDENTS"
                )
            }

    @PostMapping("/students/search")
    fun getStudents(
        user: AuthenticatedUser,
        @RequestBody body: StudentSearchParams
    ): List<StudentSummary> =
        db()
            .connect { it.read { tx ->
                tx.getStudentSummaries(
                    params =
                        body.copy(
                            query = body.query.takeIf { !it.isNullOrBlank() }
                        )
                )
            } }.also {
                logger.audit(
                    user,
                    "SEARCH_STUDENTS"
                )
            }

    data class StudentResponse(
        val student: Student,
        val cases: List<StudentCase>
    )

    @GetMapping("/students/{id}")
    fun getStudent(
        user: AuthenticatedUser,
        @PathVariable id: StudentId
    ): StudentResponse =
        db()
            .connect { it.read { tx ->
                val studentDetails = tx.getStudent(id = id)
                val cases = tx.getStudentCasesByStudent(studentId = id)
                StudentResponse(studentDetails, cases)
            } }.also {
                logger.audit(
                    user,
                    "GET_STUDENT",
                    mapOf("studentId" to id.toString())
                )
            }

    @PutMapping("/students/{id}")
    fun updateStudent(
        user: AuthenticatedUser,
        @PathVariable id: StudentId,
        @RequestBody body: StudentInput
    ) {
        db()
            .connect { it.transaction { tx ->
                tx.updateStudent(id = id, data = body, user = user)
            } }.also {
                logger.audit(
                    user,
                    "UPDATE_STUDENT",
                    mapOf("studentId" to id.toString())
                )
            }
    }

    @DeleteMapping("/students/{id}")
    fun deleteStudent(
        user: AuthenticatedUser,
        @PathVariable id: StudentId
    ) {
        db()
            .connect { it.transaction { tx ->
                tx.deleteStudent(id = id)
            } }.also {
                logger.audit(
                    user,
                    "DELETE_STUDENT",
                    mapOf("studentId" to id.toString())
                )
            }
    }

    @PostMapping("/students/{studentId}/cases")
    fun createStudentCase(
        user: AuthenticatedUser,
        @PathVariable studentId: StudentId,
        @RequestBody body: StudentCaseInput
    ): StudentCaseId =
        db()
            .connect { it.transaction { tx ->
                tx.insertStudentCase(studentId = studentId, data = body, user = user)
            } }.also {
                logger.audit(
                    user,
                    "CREATE_STUDENT_CASE",
                    mapOf("studentId" to studentId.toString())
                )
            }

    @PutMapping("/students/{studentId}/cases/{id}")
    fun updateStudentCase(
        user: AuthenticatedUser,
        @PathVariable studentId: StudentId,
        @PathVariable id: StudentCaseId,
        @RequestBody body: StudentCaseInput
    ) {
        db()
            .connect { it.transaction { tx ->
                tx.updateStudentCase(id = id, studentId = studentId, data = body, user = user)
            } }.also {
                logger.audit(
                    user,
                    "UPDATE_STUDENT_CASE",
                    mapOf("studentId" to studentId.toString(), "caseId" to id.toString())
                )
            }
    }

    @DeleteMapping("/students/{studentId}/cases/{id}")
    fun deleteStudentCase(
        user: AuthenticatedUser,
        @PathVariable studentId: StudentId,
        @PathVariable id: StudentCaseId
    ) {
        db()
            .connect { it.transaction { tx ->
                tx.deleteStudentCase(id = id, studentId = studentId)
            } }.also {
                logger.audit(
                    user,
                    "DELETE_STUDENT_CASE",
                    mapOf("studentId" to studentId.toString(), "caseId" to id.toString())
                )
            }
    }

    @PutMapping("/students/{studentId}/cases/{id}/status")
    fun updateStudentCaseStatus(
        user: AuthenticatedUser,
        @PathVariable studentId: StudentId,
        @PathVariable id: StudentCaseId,
        @RequestBody body: CaseStatusInput
    ) {
        db()
            .connect { it.transaction { tx ->
                tx.updateStudentCaseStatus(id = id, studentId = studentId, data = body, user = user)
            } }.also {
                logger.audit(
                    user,
                    "UPDATE_STUDENT_CASE_STATUS",
                    mapOf("studentId" to studentId.toString(), "caseId" to id.toString())
                )
            }
    }

    @PostMapping("/student-cases/{studentCaseId}/case-events")
    fun createCaseEvent(
        user: AuthenticatedUser,
        @PathVariable studentCaseId: StudentCaseId,
        @RequestBody body: CaseEventInput
    ): CaseEventId =
        db()
            .connect { it.transaction { tx ->
                tx.insertCaseEvent(studentCaseId = studentCaseId, data = body, user = user)
            } }.also {
                logger.audit(
                    user,
                    "CREATE_CASE_EVENT",
                    mapOf("caseId" to studentCaseId.toString())
                )
            }

    @PutMapping("/case-events/{id}")
    fun updateCaseEvent(
        user: AuthenticatedUser,
        @PathVariable id: CaseEventId,
        @RequestBody body: CaseEventInput
    ) {
        db()
            .connect { it.transaction { tx ->
                tx.updateCaseEvent(id = id, data = body, user = user)
            } }.also {
                logger.audit(
                    user,
                    "UPDATE_CASE_EVENT",
                    mapOf("eventId" to id.toString())
                )
            }
    }

    @DeleteMapping("/case-events/{id}")
    fun deleteCaseEvent(
        user: AuthenticatedUser,
        @PathVariable id: CaseEventId
    ) {
        db()
            .connect { it.transaction { tx ->
                tx.deleteCaseEvent(id = id)
            } }.also {
                logger.audit(
                    user,
                    "DELETE_CASE_EVENT",
                    mapOf("eventId" to id.toString())
                )
            }
    }

    @GetMapping("/employees")
    fun getEmployeeUsers(user: AuthenticatedUser): List<AppUser> =
        db().connect { it.read { tx -> tx.getActiveAppUsers() } }.also {
            logger.audit(
                user,
                "GET_EMPLOYEES"
            )
        }

    @GetMapping("/reports/student-cases")
    fun getCasesReport(
        user: AuthenticatedUser,
        @RequestParam(required = false) start: LocalDate?,
        @RequestParam(required = false) end: LocalDate?
    ): List<CaseReportRow> =
        db().connect { it.read { tx -> tx.getCasesReport(CaseReportRequest(start, end)) } }.also {
            logger.audit(
                user,
                "GET_CASES_REPORT"
            )
        }

    @DeleteMapping("/old-students")
    fun deleteOldStudents(user: AuthenticatedUser) =
        db().connect { it.transaction { tx -> tx.deleteOldStudents() } }.also {
            logger.audit(
                user,
                "DELETE_OLD_STUDENTS"
            )
        }
}
