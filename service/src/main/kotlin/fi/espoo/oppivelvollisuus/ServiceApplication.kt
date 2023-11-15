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
class HelloWorldController {
    @Autowired
    lateinit var jdbi: Jdbi

    data class StudentInput(
        val firstName: String,
        val lastName: String
    )
    @PostMapping("/students")
    fun createStudent(@RequestBody body: StudentInput): UUID {
        val id = UUID.randomUUID()
        jdbi.inTransactionUnchecked { tx ->
            tx.createUpdate("""
                INSERT INTO students (id, first_name, last_name) 
                VALUES (:id, :firstName, :lastName)
            """)
                .bind("id", id)
                .bindKotlin(body)
                .execute()
        }
        return id
    }

    data class StudentBasics(
        val id: UUID,
        val firstName: String,
        val lastName: String
    )
    @GetMapping("/students")
    fun getStudents(): List<StudentBasics> {
        return jdbi.inTransactionUnchecked { tx ->
            tx.createQuery("SELECT id, first_name, last_name FROM students")
                .mapTo<StudentBasics>()
                .list()
        }
    }
    @GetMapping("/students/{id}")
    fun getStudent(@PathVariable id: UUID): StudentBasics {
        return jdbi.inTransactionUnchecked { tx ->
            tx.createQuery("""
                SELECT id, first_name, last_name 
                FROM students
                WHERE id = :id
            """.trimIndent())
                .bind("id", id)
                .mapTo<StudentBasics>()
                .findOne()
                .getOrNull()
                ?: error("not found")
        }
    }

    @PutMapping("/students/{id}")
    fun updateStudent(@PathVariable id: UUID, @RequestBody body: StudentInput): Unit {
        jdbi.inTransactionUnchecked { tx ->
            tx.createUpdate("""
                UPDATE students 
                SET first_name = :firstName, last_name = :lastName
                WHERE id = :id
            """)
                .bind("id", id)
                .bindKotlin(body)
                .execute()
                .also { if(it != 1) error("not found") }
        }
    }

    data class StudentCaseInput(
        val openedAt: LocalDate,
        val info: String
    )
    @PostMapping("/students/{studentId}/cases")
    fun createStudentCase(@PathVariable studentId: UUID, @RequestBody body: StudentCaseInput): UUID {
        val id = UUID.randomUUID()
        jdbi.inTransactionUnchecked { tx ->
            tx.createUpdate("""
                INSERT INTO student_cases (id, student_id, opened_at, info) 
                VALUES (:id, :studentId, :openedAt, :info)
            """)
                .bind("id", id)
                .bind("studentId", studentId)
                .bindKotlin(body)
                .execute()
        }
        return id
    }

    data class StudentCase(
        val id: UUID,
        val studentId: UUID,
        val openedAt: LocalDate,
        val info: String
    )
    @GetMapping("/students/{studentId}/cases")
    fun getStudentCasesByStudent(@PathVariable studentId: UUID): List<StudentCase> {
        return jdbi.inTransactionUnchecked { tx ->
            tx.createQuery("""
                SELECT id, student_id, opened_at, info
                FROM student_cases
                WHERE student_id = :studentId
            """.trimIndent())
                .bind("studentId", studentId)
                .mapTo<StudentCase>()
                .list()
        }
    }

    @PutMapping("/students/{studentId}/cases/{id}")
    fun updateStudentCase(
        @PathVariable studentId: UUID,
        @PathVariable id: UUID,
        @RequestBody body: StudentCaseInput
    ) {
        jdbi.inTransactionUnchecked { tx ->
            tx.createUpdate("""
                UPDATE student_cases
                SET 
                    opened_at = :openedAt,
                    info = :info
                WHERE id = :id AND student_id = :studentId
            """)
                .bind("id", id)
                .bind("studentId", studentId)
                .bindKotlin(body)
                .execute()
                .also { if(it != 1) error("not found") }
        }
    }

    data class StudentCaseSummary(
        val id: UUID,
        val studentId: UUID,
        val firstName: String,
        val lastName: String,
        val openedAt: LocalDate
    )
    @GetMapping("/students-cases")
    fun getStudentCases(): List<StudentCaseSummary> {
        return jdbi.inTransactionUnchecked { tx ->
            tx.createQuery("""
                SELECT c.id, student_id, first_name, last_name, opened_at, info
                FROM student_cases c
                JOIN students s on s.id = c.student_id
            """.trimIndent())
                .mapTo<StudentCaseSummary>()
                .list()
        }
    }

}
