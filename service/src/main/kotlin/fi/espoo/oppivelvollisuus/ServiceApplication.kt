package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@SpringBootApplication
class ServiceApplication

fun main(args: Array<String>) {
    runApplication<ServiceApplication>(*args)
}

@RestController
class MainController {
    @Autowired
    lateinit var jdbi: Jdbi

    data class StudentInput(
        val valpasLink: String,
        val ssn: String,
        val firstName: String,
        val lastName: String,
        val dateOfBirth: LocalDate?
    )

    @PostMapping("/students")
    fun createStudent(@RequestBody body: StudentInput): UUID {
        val id = UUID.randomUUID()
        jdbi.inTransactionUnchecked { tx ->
            tx.createUpdate(
                """
                INSERT INTO students (id, valpas_link, ssn, first_name, last_name, date_of_birth) 
                VALUES (:id, :valpasLink, :ssn, :firstName, :lastName, :dateOfBirth)
            """
            )
                .bind("id", id)
                .bindKotlin(body)
                .execute()
        }
        return id
    }

    data class StudentSummary(
        val id: UUID,
        val firstName: String,
        val lastName: String,
        val openedAt: LocalDate?
    )

    @GetMapping("/students")
    fun getStudents(): List<StudentSummary> {
        return jdbi.inTransactionUnchecked { tx ->
            tx.createQuery(
                """
                SELECT id, first_name, last_name, sc.opened_at
                FROM students
                LEFT JOIN LATERAL (
                    SELECT opened_at
                    FROM student_cases
                    WHERE student_id = students.id
                    ORDER BY opened_at DESC
                    LIMIT 1
                ) sc ON true
                ORDER BY opened_at DESC, first_name, last_name
                """.trimIndent()
            )
                .mapTo<StudentSummary>()
                .list()
        }
    }

    data class StudentDetails(
        val id: UUID,
        val valpasLink: String,
        val ssn: String,
        val firstName: String,
        val lastName: String,
        val dateOfBirth: LocalDate?
    )

    data class StudentResponse(
        val student: StudentDetails,
        val cases: List<StudentCase>
    )

    @GetMapping("/students/{id}")
    fun getStudent(@PathVariable id: UUID): StudentResponse {
        return jdbi.inTransactionUnchecked { tx ->
            val studentDetails = tx.createQuery(
                """
                SELECT id, valpas_link, ssn, first_name, last_name, date_of_birth
                FROM students
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .mapTo<StudentDetails>()
                .findOne()
                .getOrNull()
                ?: error("not found")

            val cases = tx.createQuery(
                """
                SELECT id, student_id, opened_at, info
                FROM student_cases
                WHERE student_id = :studentId
                """.trimIndent()
            )
                .bind("studentId", id)
                .mapTo<StudentCase>()
                .list()

            StudentResponse(studentDetails, cases)
        }
    }

    @PutMapping("/students/{id}")
    fun updateStudent(@PathVariable id: UUID, @RequestBody body: StudentInput) {
        jdbi.inTransactionUnchecked { tx ->
            tx.createUpdate(
                """
                UPDATE students 
                SET 
                    valpas_link = :valpasLink,
                    ssn = :ssn,
                    first_name = :firstName,
                    last_name = :lastName,
                    date_of_birth = :dateOfBirth
                WHERE id = :id
            """
            )
                .bind("id", id)
                .bindKotlin(body)
                .execute()
                .also { if (it != 1) error("not found") }
        }
    }

    data class StudentCase(
        val id: UUID,
        val studentId: UUID,
        val openedAt: LocalDate,
        val info: String
    )

    data class StudentCaseInput(
        val openedAt: LocalDate,
        val info: String
    )

    @PostMapping("/students/{studentId}/cases")
    fun createStudentCase(@PathVariable studentId: UUID, @RequestBody body: StudentCaseInput): UUID {
        val id = UUID.randomUUID()
        jdbi.inTransactionUnchecked { tx ->
            tx.createUpdate(
                """
                INSERT INTO student_cases (id, student_id, opened_at, info) 
                VALUES (:id, :studentId, :openedAt, :info)
            """
            )
                .bind("id", id)
                .bind("studentId", studentId)
                .bindKotlin(body)
                .execute()
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
            tx.createUpdate(
                """
                UPDATE student_cases
                SET 
                    opened_at = :openedAt,
                    info = :info
                WHERE id = :id AND student_id = :studentId
            """
            )
                .bind("id", id)
                .bind("studentId", studentId)
                .bindKotlin(body)
                .execute()
                .also { if (it != 1) error("not found") }
        }
    }
}
