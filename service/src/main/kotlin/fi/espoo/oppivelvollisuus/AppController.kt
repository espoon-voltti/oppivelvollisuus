package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.springframework.beans.factory.annotation.Autowired
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
    fun createStudent(@RequestBody body: StudentAndCaseInput): UUID {
        val studentId = UUID.randomUUID()
        val studentCaseId = UUID.randomUUID()
        jdbi.inTransactionUnchecked { tx ->
            tx.insertStudent(id = studentId, data = body.student)
            tx.insertStudentCase(id = studentCaseId, studentId = studentId, data = body.studentCase)
        }
        return studentId
    }

    @GetMapping("/students")
    fun getStudents(): List<StudentSummary> {
        return jdbi.inTransactionUnchecked { tx ->
            tx.getStudentSummaries()
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
    fun updateStudent(@PathVariable id: UUID, @RequestBody body: StudentInput) {
        jdbi.inTransactionUnchecked { tx ->
            tx.updateStudent(id = id, data = body)
        }
    }

    @PostMapping("/students/{studentId}/cases")
    fun createStudentCase(@PathVariable studentId: UUID, @RequestBody body: StudentCaseInput): UUID {
        val id = UUID.randomUUID()
        jdbi.inTransactionUnchecked { tx ->
            tx.insertStudentCase(id = id, studentId = studentId, data = body)
        }
        return id
    }

    @PutMapping("/students/{studentId}/cases/{id}")
    fun updateStudentCase(
        @PathVariable studentId: UUID,
        @PathVariable id: UUID,
        @RequestBody body: StudentCaseInput
    ) {
        jdbi.inTransactionUnchecked { tx ->
            tx.updateStudentCase(id = id, studentId = studentId, data = body)
        }
    }

    @GetMapping("/employees")
    fun getEmployees(): List<EmployeeUser> {
        return jdbi.inTransactionUnchecked { it.getEmployees() }
    }
}
