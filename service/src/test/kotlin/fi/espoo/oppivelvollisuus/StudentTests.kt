package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.common.NotFound
import fi.espoo.oppivelvollisuus.common.UserBasics
import fi.espoo.oppivelvollisuus.common.isUniqueConstraintViolation
import fi.espoo.oppivelvollisuus.domain.AppController
import fi.espoo.oppivelvollisuus.domain.CaseBackgroundReason
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.domain.CaseStatus
import fi.espoo.oppivelvollisuus.domain.DuplicateStudentCheckInput
import fi.espoo.oppivelvollisuus.domain.Gender
import fi.espoo.oppivelvollisuus.domain.NotInSchoolReason
import fi.espoo.oppivelvollisuus.domain.SchoolBackground
import fi.espoo.oppivelvollisuus.domain.Student
import fi.espoo.oppivelvollisuus.domain.StudentCase
import fi.espoo.oppivelvollisuus.domain.StudentCaseInput
import fi.espoo.oppivelvollisuus.domain.StudentInput
import fi.espoo.oppivelvollisuus.domain.StudentSearchParams
import fi.espoo.oppivelvollisuus.domain.StudentSummary
import fi.espoo.oppivelvollisuus.domain.ValpasNotifier
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudentTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: AppController

    val emptySearch =
        StudentSearchParams(
            query = "",
            statuses = CaseStatus.entries,
            assignedTo = null
        )

    @Test
    fun `get empty list of students`() {
        assertEquals(emptyList(), controller.getStudents(testUser, emptySearch))
    }

    @Test
    fun `create student with all data and fetch`() {
        val studentId =
            controller.createStudent(
                user = testUser,
                body =
                    AppController.StudentAndCaseInput(
                        student =
                            StudentInput(
                                valpasLink = "valpas",
                                ssn = "170108A927R",
                                firstName = "Testi",
                                lastName = "Testilä",
                                language = "suomi",
                                dateOfBirth = LocalDate.of(2008, 1, 17),
                                phone = "1234567",
                                email = "a@a.com",
                                gender = Gender.FEMALE,
                                address = "Katu 1",
                                municipalityInFinland = false,
                                guardianInfo = "Huoltaja",
                                supportContactsInfo = "Joku muu"
                            ),
                        studentCase =
                            StudentCaseInput(
                                openedAt = LocalDate.of(2023, 12, 7),
                                assignedTo = testUser.id,
                                source = CaseSource.VALPAS_NOTICE,
                                sourceValpas = ValpasNotifier.PERUSOPETUS,
                                sourceOther = null,
                                sourceContact = "Espoon ala-aste",
                                schoolBackground = SchoolBackground.entries.toSet(),
                                caseBackgroundReasons = CaseBackgroundReason.entries.toSet(),
                                notInSchoolReason = NotInSchoolReason.KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA
                            )
                    )
            )
        assertEquals(
            expected =
                listOf(
                    StudentSummary(
                        id = studentId,
                        firstName = "Testi",
                        lastName = "Testilä",
                        openedAt = LocalDate.of(2023, 12, 7),
                        assignedTo = UserBasics(id = testUser.id, name = testUserName),
                        status = CaseStatus.TODO
                    )
                ),
            actual = controller.getStudents(testUser, emptySearch)
        )

        val studentResponse = controller.getStudent(testUser, studentId)
        assertEquals(
            Student(
                id = studentId,
                valpasLink = "valpas",
                ssn = "170108A927R",
                firstName = "Testi",
                lastName = "Testilä",
                language = "suomi",
                dateOfBirth = LocalDate.of(2008, 1, 17),
                phone = "1234567",
                email = "a@a.com",
                gender = Gender.FEMALE,
                address = "Katu 1",
                municipalityInFinland = false,
                guardianInfo = "Huoltaja",
                supportContactsInfo = "Joku muu"
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
                    assignedTo = UserBasics(id = testUser.id, name = testUserName),
                    status = CaseStatus.TODO,
                    finishedInfo = null,
                    source = CaseSource.VALPAS_NOTICE,
                    sourceValpas = ValpasNotifier.PERUSOPETUS,
                    sourceOther = null,
                    sourceContact = "Espoon ala-aste",
                    schoolBackground = SchoolBackground.entries.toSet(),
                    caseBackgroundReasons = CaseBackgroundReason.entries.toSet(),
                    notInSchoolReason = NotInSchoolReason.KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA,
                    events = emptyList()
                ),
                studentCase
            )
        }
    }

    @Test
    fun `create student with minimal data and fetch`() {
        val studentId =
            controller.createStudent(
                user = testUser,
                body =
                    AppController.StudentAndCaseInput(
                        student =
                            StudentInput(
                                valpasLink = "",
                                ssn = "",
                                firstName = "Testi",
                                lastName = "Testilä",
                                language = "",
                                dateOfBirth = null,
                                phone = "",
                                email = "",
                                gender = null,
                                address = "",
                                municipalityInFinland = true,
                                guardianInfo = "",
                                supportContactsInfo = ""
                            ),
                        studentCase =
                            StudentCaseInput(
                                openedAt = LocalDate.of(2023, 12, 7),
                                assignedTo = null,
                                source = CaseSource.VALPAS_AUTOMATIC_CHECK,
                                sourceValpas = null,
                                sourceOther = null,
                                sourceContact = "",
                                schoolBackground = emptySet(),
                                caseBackgroundReasons = emptySet(),
                                notInSchoolReason = null
                            )
                    )
            )
        assertEquals(
            expected =
                listOf(
                    StudentSummary(
                        id = studentId,
                        firstName = "Testi",
                        lastName = "Testilä",
                        openedAt = LocalDate.of(2023, 12, 7),
                        assignedTo = null,
                        status = CaseStatus.TODO
                    )
                ),
            actual = controller.getStudents(testUser, emptySearch)
        )

        val studentResponse = controller.getStudent(testUser, studentId)
        assertEquals(
            Student(
                id = studentId,
                valpasLink = "",
                ssn = "",
                firstName = "Testi",
                lastName = "Testilä",
                language = "",
                dateOfBirth = null,
                phone = "",
                email = "",
                gender = null,
                address = "",
                municipalityInFinland = true,
                guardianInfo = "",
                supportContactsInfo = ""
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
                    assignedTo = null,
                    status = CaseStatus.TODO,
                    finishedInfo = null,
                    source = CaseSource.VALPAS_AUTOMATIC_CHECK,
                    sourceValpas = null,
                    sourceOther = null,
                    sourceContact = "",
                    schoolBackground = emptySet(),
                    caseBackgroundReasons = emptySet(),
                    notInSchoolReason = null,
                    events = emptyList()
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
                language = "ruotsi",
                dateOfBirth = LocalDate.of(2008, 1, 17),
                phone = "1234567",
                email = "a@a.com",
                gender = Gender.MALE,
                address = "Katu 1",
                municipalityInFinland = false,
                guardianInfo = "Huoltaja",
                supportContactsInfo = "Opo"
            )
        )

        val studentResponse = controller.getStudent(testUser, studentId)
        assertEquals(
            Student(
                id = studentId,
                valpasLink = "valpas",
                ssn = "170108A927R",
                firstName = "Teppo",
                lastName = "Testaajainen",
                language = "ruotsi",
                dateOfBirth = LocalDate.of(2008, 1, 17),
                phone = "1234567",
                email = "a@a.com",
                gender = Gender.MALE,
                address = "Katu 1",
                municipalityInFinland = false,
                guardianInfo = "Huoltaja",
                supportContactsInfo = "Opo"
            ),
            studentResponse.student
        )
    }

    @Test
    fun `creating two people with same is ok`() {
        controller.createStudent(
            user = testUser,
            body =
                AppController.StudentAndCaseInput(
                    student = minimalStudentTestInput,
                    studentCase = minimalStudentCaseTestInput
                )
        )
        controller.createStudent(
            user = testUser,
            body =
                AppController.StudentAndCaseInput(
                    student = minimalStudentTestInput,
                    studentCase = minimalStudentCaseTestInput
                )
        )

        assertEquals(2, controller.getStudents(testUser, emptySearch).size)
    }

    @Test
    fun `creating two people with same ssn fails`() {
        controller.createStudent(
            user = testUser,
            body =
                AppController.StudentAndCaseInput(
                    student =
                        minimalStudentTestInput.copy(
                            ssn = "170108A927R"
                        ),
                    studentCase = minimalStudentCaseTestInput
                )
        )
        val e =
            assertThrows<UnableToExecuteStatementException> {
                controller.createStudent(
                    user = testUser,
                    body =
                        AppController.StudentAndCaseInput(
                            student =
                                minimalStudentTestInput.copy(
                                    ssn = "170108A927R"
                                ),
                            studentCase = minimalStudentCaseTestInput
                        )
                )
            }
        assertTrue(e.isUniqueConstraintViolation())

        assertEquals(1, controller.getStudents(testUser, emptySearch).size)
    }

    @Test
    fun `creating two people with same valpas link fails`() {
        controller.createStudent(
            user = testUser,
            body =
                AppController.StudentAndCaseInput(
                    student =
                        minimalStudentTestInput.copy(
                            valpasLink = "http://valpas.fi/123"
                        ),
                    studentCase = minimalStudentCaseTestInput
                )
        )
        val e =
            assertThrows<UnableToExecuteStatementException> {
                controller.createStudent(
                    user = testUser,
                    body =
                        AppController.StudentAndCaseInput(
                            student =
                                minimalStudentTestInput.copy(
                                    valpasLink = "http://valpas.fi/123"
                                ),
                            studentCase = minimalStudentCaseTestInput
                        )
                )
            }
        assertTrue(e.isUniqueConstraintViolation())

        assertEquals(1, controller.getStudents(testUser, emptySearch).size)
    }

    @Test
    fun `duplicate ssn is detected`() {
        controller.createStudent(
            user = testUser,
            body =
                AppController.StudentAndCaseInput(
                    student =
                        minimalStudentTestInput.copy(
                            ssn = "170108A927R"
                        ),
                    studentCase = minimalStudentCaseTestInput
                )
        )
        val duplicateStudents =
            controller.getDuplicateStudents(
                testUser,
                DuplicateStudentCheckInput(
                    ssn = "170108A927R",
                    valpasLink = "",
                    firstName = "",
                    lastName = ""
                )
            )
        assertEquals(1, duplicateStudents.size)
        duplicateStudents.first().let { duplicate ->
            assertTrue(duplicate.matchingSsn)
            assertFalse(duplicate.matchingValpasLink)
            assertFalse(duplicate.matchingName)
        }
    }

    @Test
    fun `duplicate valpasLink is detected`() {
        controller.createStudent(
            user = testUser,
            body =
                AppController.StudentAndCaseInput(
                    student =
                        minimalStudentTestInput.copy(
                            valpasLink = "https://valpas.fi/123"
                        ),
                    studentCase = minimalStudentCaseTestInput
                )
        )
        val duplicateStudents =
            controller.getDuplicateStudents(
                testUser,
                DuplicateStudentCheckInput(
                    ssn = "",
                    valpasLink = "https://valpas.fi/123",
                    firstName = "",
                    lastName = ""
                )
            )
        assertEquals(1, duplicateStudents.size)
        duplicateStudents.first().let { duplicate ->
            assertFalse(duplicate.matchingSsn)
            assertTrue(duplicate.matchingValpasLink)
            assertFalse(duplicate.matchingName)
        }
    }

    @Test
    fun `duplicate name is detected`() {
        controller.createStudent(
            user = testUser,
            body =
                AppController.StudentAndCaseInput(
                    student =
                        minimalStudentTestInput.copy(
                            firstName = "Tupu",
                            lastName = "Ankka"
                        ),
                    studentCase = minimalStudentCaseTestInput
                )
        )
        val duplicateStudents =
            controller.getDuplicateStudents(
                testUser,
                DuplicateStudentCheckInput(
                    ssn = "",
                    valpasLink = "",
                    firstName = "Tupu",
                    lastName = "Ankka"
                )
            )
        assertEquals(1, duplicateStudents.size)
        duplicateStudents.first().let { duplicate ->
            assertFalse(duplicate.matchingSsn)
            assertFalse(duplicate.matchingValpasLink)
            assertTrue(duplicate.matchingName)
        }
    }

    @Test
    fun `duplicate name is ignored if both students have ssn`() {
        controller.createStudent(
            user = testUser,
            body =
                AppController.StudentAndCaseInput(
                    student =
                        minimalStudentTestInput.copy(
                            ssn = "170108A927R",
                            firstName = "Tupu",
                            lastName = "Ankka"
                        ),
                    studentCase = minimalStudentCaseTestInput
                )
        )
        val duplicateStudents =
            controller.getDuplicateStudents(
                testUser,
                DuplicateStudentCheckInput(
                    ssn = "100507A967F",
                    valpasLink = "",
                    firstName = "Tupu",
                    lastName = "Ankka"
                )
            )
        assertEquals(0, duplicateStudents.size)
    }

    @Test
    fun `deleting student fails when it has cases`() {
        val studentId =
            controller.createStudent(
                user = testUser,
                body = minimalStudentAndCaseTestInput
            )
        assertThrows<UnableToExecuteStatementException> {
            controller.deleteStudent(testUser, studentId)
        }
    }

    @Test
    fun `deleting student after deleting its cases`() {
        val studentId =
            controller.createStudent(
                user = testUser,
                body = minimalStudentAndCaseTestInput
            )
        val caseId = controller.getStudent(testUser, studentId).cases.first().id
        controller.deleteStudentCase(testUser, studentId, caseId)

        controller.deleteStudent(testUser, studentId)

        assertEquals(0, controller.getStudents(testUser, emptySearch).size)
        assertThrows<NotFound> { controller.getStudent(testUser, studentId) }
    }
}
