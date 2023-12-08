import fi.espoo.oppivelvollisuus.config.AuthenticatedUser
import fi.espoo.oppivelvollisuus.domain.AppController
import fi.espoo.oppivelvollisuus.domain.StudentCaseInput
import fi.espoo.oppivelvollisuus.domain.StudentInput
import java.time.LocalDate
import java.util.*

val testUser = AuthenticatedUser(UUID.randomUUID())
val testUserName = "Teija Testaaja"

val minimalStudentTestInput = StudentInput(
    valpasLink = "",
    ssn = "",
    firstName = "Testi",
    lastName = "Testilä",
    dateOfBirth = null,
    phone = "",
    email = "",
    address = ""
)
val minimalStudentCaseTestInput = StudentCaseInput(
    openedAt = LocalDate.of(2023, 12, 7),
    assignedTo = null
)
val minimalStudentAndCaseTestInput = AppController.StudentAndCaseInput(
    student = minimalStudentTestInput,
    studentCase = minimalStudentCaseTestInput
)
