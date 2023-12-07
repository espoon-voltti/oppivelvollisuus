package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.common.UserBasics
import fi.espoo.oppivelvollisuus.common.isUniqueConstraintViolation
import fi.espoo.oppivelvollisuus.domain.AppController
import fi.espoo.oppivelvollisuus.domain.Student
import fi.espoo.oppivelvollisuus.domain.StudentCase
import fi.espoo.oppivelvollisuus.domain.StudentCaseInput
import fi.espoo.oppivelvollisuus.domain.StudentInput
import fi.espoo.oppivelvollisuus.domain.StudentSummary
import minimalStudentAndCaseTestInput
import minimalStudentCaseTestInput
import minimalStudentTestInput
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import testUser
import testUserName
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudentTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: AppController

    @Test
    fun `get empty list of students`() {
        assertEquals(emptyList(), controller.getStudents())
    }

    @Test
    fun `create student with all data and fetch`() {
        val studentId = controller.createStudent(
            user = testUser,
            body = AppController.StudentAndCaseInput(
                student = StudentInput(
                    valpasLink = "valpas",
                    ssn = "170108A927R",
                    firstName = "Testi",
                    lastName = "Testilä",
                    dateOfBirth = LocalDate.of(2008, 1, 17),
                    phone = "1234567",
                    email = "a@a.com",
                    address = "Katu 1"
                ),
                studentCase = StudentCaseInput(
                    openedAt = LocalDate.of(2023, 12, 7),
                    assignedTo = testUser.id
                )
            )
        )
        assertEquals(
            expected = listOf(
                StudentSummary(
                    id = studentId,
                    firstName = "Testi",
                    lastName = "Testilä",
                    openedAt = LocalDate.of(2023, 12, 7),
                    assignedTo = UserBasics(id = testUser.id, name = testUserName)
                )
            ),
            actual = controller.getStudents()
        )

        val studentResponse = controller.getStudent(studentId)
        assertEquals(
            Student(
                id = studentId,
                valpasLink = "valpas",
                ssn = "170108A927R",
                firstName = "Testi",
                lastName = "Testilä",
                dateOfBirth = LocalDate.of(2008, 1, 17),
                phone = "1234567",
                email = "a@a.com",
                address = "Katu 1"
            ),
            studentResponse.student
        )
        assertEquals(1, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = studentCase.id,
                    studentId = studentId,
                    openedAt = LocalDate.of(2023, 12, 7),
                    assignedTo = UserBasics(id = testUser.id, name = testUserName)
                ),
                studentCase
            )
        }
    }

    @Test
    fun `create student with minimal data and fetch`() {
        val studentId = controller.createStudent(
            user = testUser,
            body = AppController.StudentAndCaseInput(
                student = StudentInput(
                    valpasLink = "",
                    ssn = "",
                    firstName = "Testi",
                    lastName = "Testilä",
                    dateOfBirth = null,
                    phone = "",
                    email = "",
                    address = ""
                ),
                studentCase = StudentCaseInput(
                    openedAt = LocalDate.of(2023, 12, 7),
                    assignedTo = null
                )
            )
        )
        assertEquals(
            expected = listOf(
                StudentSummary(
                    id = studentId,
                    firstName = "Testi",
                    lastName = "Testilä",
                    openedAt = LocalDate.of(2023, 12, 7),
                    assignedTo = null
                )
            ),
            actual = controller.getStudents()
        )

        val studentResponse = controller.getStudent(studentId)
        assertEquals(
            Student(
                id = studentId,
                valpasLink = "",
                ssn = "",
                firstName = "Testi",
                lastName = "Testilä",
                dateOfBirth = null,
                phone = "",
                email = "",
                address = ""
            ),
            studentResponse.student
        )
        assertEquals(1, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = studentCase.id,
                    studentId = studentId,
                    openedAt = LocalDate.of(2023, 12, 7),
                    assignedTo = null
                ),
                studentCase
            )
        }
    }

    @Test
    fun `update student data`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)

        controller.updateStudent(
            user = testUser,
            id = studentId,
            StudentInput(
                valpasLink = "valpas",
                ssn = "170108A927R",
                firstName = "Teppo",
                lastName = "Testaajainen",
                dateOfBirth = LocalDate.of(2008, 1, 17),
                phone = "1234567",
                email = "a@a.com",
                address = "Katu 1"
            )
        )

        val studentResponse = controller.getStudent(studentId)
        assertEquals(
            Student(
                id = studentId,
                valpasLink = "valpas",
                ssn = "170108A927R",
                firstName = "Teppo",
                lastName = "Testaajainen",
                dateOfBirth = LocalDate.of(2008, 1, 17),
                phone = "1234567",
                email = "a@a.com",
                address = "Katu 2"
            ),
            studentResponse.student
        )
    }

    @Test
    fun `creating two people with same is ok`() {
        controller.createStudent(
            user = testUser,
            body = AppController.StudentAndCaseInput(
                student = minimalStudentTestInput,
                studentCase = minimalStudentCaseTestInput
            )
        )
        controller.createStudent(
            user = testUser,
            body = AppController.StudentAndCaseInput(
                student = minimalStudentTestInput,
                studentCase = minimalStudentCaseTestInput
            )
        )

        assertEquals(2, controller.getStudents().size)
    }

    @Test
    fun `creating two people with same ssn fails`() {
        controller.createStudent(
            user = testUser,
            body = AppController.StudentAndCaseInput(
                student = minimalStudentTestInput.copy(
                    ssn = "170108A927R"
                ),
                studentCase = minimalStudentCaseTestInput
            )
        )
        val e = assertThrows<UnableToExecuteStatementException> {
            controller.createStudent(
                user = testUser,
                body = AppController.StudentAndCaseInput(
                    student = minimalStudentTestInput.copy(
                        ssn = "170108A927R"
                    ),
                    studentCase = minimalStudentCaseTestInput
                )
            )
        }
        assertTrue(e.isUniqueConstraintViolation())

        assertEquals(1, controller.getStudents().size)
    }

    @Test
    fun `creating two people with same valpas link fails`() {
        controller.createStudent(
            user = testUser,
            body = AppController.StudentAndCaseInput(
                student = minimalStudentTestInput.copy(
                    valpasLink = "http://valpas.fi/123"
                ),
                studentCase = minimalStudentCaseTestInput
            )
        )
        val e = assertThrows<UnableToExecuteStatementException> {
            controller.createStudent(
                user = testUser,
                body = AppController.StudentAndCaseInput(
                    student = minimalStudentTestInput.copy(
                        valpasLink = "http://valpas.fi/123"
                    ),
                    studentCase = minimalStudentCaseTestInput
                )
            )
        }
        assertTrue(e.isUniqueConstraintViolation())

        assertEquals(1, controller.getStudents().size)
    }
}
