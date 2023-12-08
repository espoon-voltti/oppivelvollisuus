package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.common.UserBasics
import fi.espoo.oppivelvollisuus.domain.AppController
import fi.espoo.oppivelvollisuus.domain.StudentCase
import fi.espoo.oppivelvollisuus.domain.StudentCaseInput
import minimalStudentAndCaseTestInput
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import testUser
import testUserName
import java.time.LocalDate
import kotlin.test.assertEquals

class StudentCaseTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: AppController

    @Test
    fun `create another student case with all data`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)

        val caseId = controller.createStudentCase(
            testUser,
            studentId,
            StudentCaseInput(
                openedAt = LocalDate.of(2023, 12, 8),
                assignedTo = testUser.id
            )
        )

        val studentResponse = controller.getStudent(studentId)
        assertEquals(2, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = caseId,
                    studentId = studentId,
                    openedAt = LocalDate.of(2023, 12, 8),
                    assignedTo = UserBasics(id = testUser.id, name = testUserName)
                ),
                studentCase
            )
        }
    }

    @Test
    fun `create another student case with minimal data and update it`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)

        val caseId = controller.createStudentCase(
            testUser,
            studentId,
            StudentCaseInput(
                openedAt = LocalDate.of(2023, 12, 8),
                assignedTo = null
            )
        )

        var studentResponse = controller.getStudent(studentId)
        assertEquals(2, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = caseId,
                    studentId = studentId,
                    openedAt = LocalDate.of(2023, 12, 8),
                    assignedTo = null
                ),
                studentCase
            )
        }

        controller.updateStudentCase(
            testUser,
            studentId,
            caseId,
            StudentCaseInput(
                openedAt = LocalDate.of(2023, 12, 9),
                assignedTo = testUser.id
            )
        )

        studentResponse = controller.getStudent(studentId)
        assertEquals(2, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = caseId,
                    studentId = studentId,
                    openedAt = LocalDate.of(2023, 12, 9),
                    assignedTo = UserBasics(id = testUser.id, name = testUserName)
                ),
                studentCase
            )
        }
    }
}
