package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.common.AppUser
import fi.espoo.oppivelvollisuus.common.getAppUsers
import fi.espoo.oppivelvollisuus.config.AuthenticatedUser
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class AppController {
    @Autowired
    lateinit var jdbi: Jdbi

    data class StudentAndCaseInput(
        val student: StudentInput,
        val studentCase: StudentCaseInput
    )

    @PostMapping("/students")
    fun createStudent(user: AuthenticatedUser, @RequestBody body: StudentAndCaseInput): UUID {
        return jdbi.inTransactionUnchecked { tx ->
            val studentId = tx.insertStudent(data = body.student, user = user)
            tx.insertStudentCase(studentId = studentId, data = body.studentCase, user = user)

            studentId
        }
    }

    @PostMapping("/students/duplicates")
    fun getDuplicateStudents(
        user: AuthenticatedUser,
        @RequestBody body: DuplicateStudentCheckInput
    ): List<DuplicateStudent> {
        return jdbi.inTransactionUnchecked { tx ->
            tx.getPossibleDuplicateStudents(body)
        }
    }

    @PostMapping("/students/search")
    fun getStudents(@RequestBody body: StudentSearchParams): List<StudentSummary> {
        return jdbi.inTransactionUnchecked { tx ->
            tx.getStudentSummaries(
                params = body.copy(
                    query = body.query.takeIf { !it.isNullOrBlank() }
                )
            )
        }
    }

    data class StudentResponse(
        val student: Student,
        val cases: List<StudentCase>
    )

    @GetMapping("/students/{id}")
    fun getStudent(@PathVariable id: UUID): StudentResponse {
        return jdbi.inTransactionUnchecked { tx ->
            val studentDetails = tx.getStudent(id = id)
            val cases = tx.getStudentCasesByStudent(studentId = id)
            StudentResponse(studentDetails, cases)
        }
    }

    @PutMapping("/students/{id}")
    fun updateStudent(user: AuthenticatedUser, @PathVariable id: UUID, @RequestBody body: StudentInput) {
        jdbi.inTransactionUnchecked { tx ->
            tx.updateStudent(id = id, data = body, user = user)
        }
    }

    @DeleteMapping("/students/{id}")
    fun deleteStudent(
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ) {
        jdbi.inTransactionUnchecked { tx ->
            tx.deleteStudent(id = id)
        }
    }

    @PostMapping("/students/{studentId}/cases")
    fun createStudentCase(user: AuthenticatedUser, @PathVariable studentId: UUID, @RequestBody body: StudentCaseInput): UUID {
        return jdbi.inTransactionUnchecked { tx ->
            tx.insertStudentCase(studentId = studentId, data = body, user = user)
        }
    }

    @PutMapping("/students/{studentId}/cases/{id}")
    fun updateStudentCase(
        user: AuthenticatedUser,
        @PathVariable studentId: UUID,
        @PathVariable id: UUID,
        @RequestBody body: StudentCaseInput
    ) {
        jdbi.inTransactionUnchecked { tx ->
            tx.updateStudentCase(id = id, studentId = studentId, data = body, user = user)
        }
    }

    @DeleteMapping("/students/{studentId}/cases/{id}")
    fun deleteStudentCase(
        user: AuthenticatedUser,
        @PathVariable studentId: UUID,
        @PathVariable id: UUID
    ) {
        jdbi.inTransactionUnchecked { tx ->
            tx.deleteStudentCase(id = id, studentId = studentId)
        }
    }

    @PutMapping("/students/{studentId}/cases/{id}/status")
    fun updateStudentCaseStatus(
        user: AuthenticatedUser,
        @PathVariable studentId: UUID,
        @PathVariable id: UUID,
        @RequestBody body: CaseStatusInput
    ) {
        jdbi.inTransactionUnchecked { tx ->
            tx.updateStudentCaseStatus(id = id, studentId = studentId, data = body, user = user)
        }
    }

    @PostMapping("/student-cases/{studentCaseId}/case-events")
    fun createCaseEvent(
        user: AuthenticatedUser,
        @PathVariable studentCaseId: UUID,
        @RequestBody body: CaseEventInput
    ): UUID {
        return jdbi.inTransactionUnchecked { tx ->
            tx.insertCaseEvent(studentCaseId = studentCaseId, data = body, user = user)
        }
    }

    @PutMapping("/case-events/{id}")
    fun updateCaseEvent(
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: CaseEventInput
    ) {
        jdbi.inTransactionUnchecked { tx ->
            tx.updateCaseEvent(id = id, data = body, user = user)
        }
    }

    @DeleteMapping("/case-events/{id}")
    fun deleteCaseEvent(@PathVariable id: UUID) {
        jdbi.inTransactionUnchecked { tx ->
            tx.deleteCaseEvent(id = id)
        }
    }

    @GetMapping("/employees")
    fun getEmployeeUsers(): List<AppUser> {
        return jdbi.inTransactionUnchecked { it.getAppUsers() }
    }
}
