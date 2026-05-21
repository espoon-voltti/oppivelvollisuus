// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.FullApplicationTest
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.UserBasics
import fi.espoo.oppivelvollisuus.shared.NotFound
import fi.espoo.oppivelvollisuus.shared.dev.DevStudent
import fi.espoo.oppivelvollisuus.shared.dev.DevStudentCase
import fi.espoo.oppivelvollisuus.shared.dev.DevUser
import fi.espoo.oppivelvollisuus.shared.dev.insert
import fi.espoo.oppivelvollisuus.shared.isUniqueConstraintViolation
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import fi.espoo.oppivelvollisuus.shared.time.MockAppClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class StudentTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: AppController

    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))
    private val clock = MockAppClock(now)

    private val testUser = DevUser()

    private val emptySearch =
        StudentSearchParams(
            query = "",
            statuses = CaseStatus.entries,
            sources = CaseSource.entries,
            assignee = null,
        )

    @BeforeEach
    fun setup() {
        db.transaction { tx -> tx.insert(testUser) }
    }

    private fun createStudent(body: AppController.StudentAndCaseInput): StudentId =
        controller.createStudent(dbInstance(), testUser.user, clock, body)

    private fun getStudent(id: StudentId) = controller.getStudent(dbInstance(), testUser.user, id)

    private fun getStudents(params: StudentSearchParams = emptySearch) =
        controller.getStudents(dbInstance(), testUser.user, params)

    private fun updateStudent(id: StudentId, body: StudentInput) =
        controller.updateStudent(dbInstance(), testUser.user, clock, id, body)

    private fun deleteStudent(id: StudentId) =
        controller.deleteStudent(dbInstance(), testUser.user, id)

    private fun deleteStudentCase(studentId: StudentId, caseId: StudentCaseId) =
        controller.deleteStudentCase(dbInstance(), testUser.user, studentId, caseId)

    private fun getDuplicateStudents(body: DuplicateStudentCheckInput) =
        controller.getDuplicateStudents(dbInstance(), testUser.user, body)

    private fun createCaseEvent(caseId: StudentCaseId, body: CaseEventInput) =
        controller.createCaseEvent(dbInstance(), testUser.user, clock, caseId, body)

    private fun deleteOldStudents() =
        controller.deleteOldStudents(dbInstance(), testUser.user, clock)

    @Test
    fun `get empty list of students`() {
        assertEquals(emptyList(), getStudents())
    }

    @Test
    fun `create student with all data and fetch`() {
        val studentId =
            createStudent(
                AppController.StudentAndCaseInput(
                    student =
                        StudentInput(
                            valpasLink = "valpas",
                            valpasOppijaOid = null,
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
                            supportContactsInfo = "Joku muu",
                            partnerOrganisations =
                                setOf(
                                    PartnerOrganisation.LASTENSUOJELU,
                                    PartnerOrganisation.TERVEYDENHUOLTO,
                                ),
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
                            notInSchoolReason =
                                NotInSchoolReason.KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA,
                        ),
                )
            )
        val caseId = getStudent(studentId).cases.first().id
        createCaseEvent(
            caseId,
            CaseEventInput(
                date = LocalDate.of(2023, 12, 7),
                type = CaseEventType.HEARING_LETTER,
                notes =
                    """
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus turpis
                    sem, mattis et scelerisque quis, convallis vulputate dui. Ut eget arcu nec
                    mi maximus porta. Donec id ex eget urna cursus vehicula congue id quam.
                    Etiam id diam velit. Morbi pellentesque, tortor nec fermentum hendrerit,
                    neque purus imperdiet tortor, sed dapibus nibh tellus sit amet tortor.
                    Integer at faucibus neque. Donec pellentesque, turpis vitae commodo tempor,
                    est ipsum elementum nunc, in pretium augue turpis non nulla. Sed pulvinar
                    mollis scelerisque. Aenean tincidunt metus ut velit facilisis, in consequat
                    ex laoreet. In magna tellus, accumsan at nisl id, fermentum vehicula eros.
                    Aliquam at gravida felis, in auctor risus. Ut porttitor dignissim arcu id
                    semper. Interdum et malesuada fames ac ante ipsum primis in faucibus.
                    """
                        .trimIndent(),
            ),
        )

        assertEquals(
            expected =
                listOf(
                    StudentSummary(
                        id = studentId,
                        firstName = "Testi",
                        lastName = "Testilä",
                        openedAt = LocalDate.of(2023, 12, 7),
                        assignedTo = UserBasics(id = testUser.id, name = testUser.name),
                        status = CaseStatus.TODO,
                        source = CaseSource.VALPAS_NOTICE,
                        lastEvent =
                            CaseEventSummary(
                                date = LocalDate.of(2023, 12, 7),
                                type = CaseEventType.HEARING_LETTER,
                                notes =
                                    """
                                    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus turpis
                                    sem, mattis et...
                                    """
                                        .trimIndent(),
                            ),
                    )
                ),
            actual = getStudents(),
        )

        val studentResponse = getStudent(studentId)
        assertEquals(
            Student(
                id = studentId,
                valpasLink = "valpas",
                valpasOppijaOid = null,
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
                supportContactsInfo = "Joku muu",
                partnerOrganisations =
                    setOf(PartnerOrganisation.LASTENSUOJELU, PartnerOrganisation.TERVEYDENHUOLTO),
            ),
            studentResponse.student,
        )
        assertEquals(1, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = studentCase.id,
                    studentId = studentId,
                    openedAt = LocalDate.of(2023, 12, 7),
                    assignedTo = UserBasics(id = testUser.id, name = testUser.name),
                    status = CaseStatus.TODO,
                    finishedInfo = null,
                    source = CaseSource.VALPAS_NOTICE,
                    sourceValpas = ValpasNotifier.PERUSOPETUS,
                    sourceOther = null,
                    sourceContact = "Espoon ala-aste",
                    schoolBackground = SchoolBackground.entries.toSet(),
                    caseBackgroundReasons = CaseBackgroundReason.entries.toSet(),
                    notInSchoolReason = NotInSchoolReason.KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA,
                    valpasNotificationId = null,
                    events = studentCase.events,
                ),
                studentCase,
            )
        }
    }

    @Test
    fun `create student with minimal data and fetch`() {
        val studentId =
            createStudent(
                AppController.StudentAndCaseInput(
                    student =
                        StudentInput(
                            valpasLink = "",
                            valpasOppijaOid = null,
                            ssn = "",
                            firstName = "Testi",
                            lastName = "Testilä",
                            language = "",
                            dateOfBirth = LocalDate.now(),
                            phone = "",
                            email = "",
                            gender = null,
                            address = "",
                            municipalityInFinland = true,
                            guardianInfo = "",
                            supportContactsInfo = "",
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
                            notInSchoolReason = null,
                        ),
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
                        status = CaseStatus.TODO,
                        source = CaseSource.VALPAS_AUTOMATIC_CHECK,
                        lastEvent = null,
                    )
                ),
            actual = getStudents(),
        )

        val studentResponse = getStudent(studentId)
        assertEquals(
            Student(
                id = studentId,
                valpasLink = "",
                valpasOppijaOid = null,
                ssn = "",
                firstName = "Testi",
                lastName = "Testilä",
                language = "",
                dateOfBirth = LocalDate.now(),
                phone = "",
                email = "",
                gender = null,
                address = "",
                municipalityInFinland = true,
                guardianInfo = "",
                supportContactsInfo = "",
            ),
            studentResponse.student,
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
                    valpasNotificationId = null,
                    events = emptyList(),
                ),
                studentCase,
            )
        }
    }

    @Test
    fun `update student data`() {
        val student = DevStudent(createdBy = testUser.id, created = now)
        db.transaction { tx ->
            tx.insert(student)
            tx.insert(
                DevStudentCase(studentId = student.id, createdBy = testUser.id, created = now)
            )
        }

        updateStudent(
            student.id,
            StudentInput(
                valpasLink = "valpas",
                valpasOppijaOid = null,
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
                supportContactsInfo = "Opo",
                partnerOrganisations =
                    setOf(
                        PartnerOrganisation.TUKIHENKILO,
                        PartnerOrganisation.MIELENTERVEYSPALVELUT,
                    ),
            ),
        )

        val studentResponse = getStudent(student.id)
        assertEquals(
            Student(
                id = student.id,
                valpasLink = "valpas",
                valpasOppijaOid = null,
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
                supportContactsInfo = "Opo",
                partnerOrganisations =
                    setOf(
                        PartnerOrganisation.TUKIHENKILO,
                        PartnerOrganisation.MIELENTERVEYSPALVELUT,
                    ),
            ),
            studentResponse.student,
        )
    }

    @Test
    fun `creating two people with same is ok`() {
        createStudent(minimalStudentAndCaseInput())
        createStudent(minimalStudentAndCaseInput())

        assertEquals(2, getStudents().size)
    }

    @Test
    fun `creating two people with same ssn fails`() {
        createStudent(minimalStudentAndCaseInput(ssn = "170108A927R"))
        val e =
            assertThrows<UnableToExecuteStatementException> {
                createStudent(minimalStudentAndCaseInput(ssn = "170108A927R"))
            }
        assertTrue(e.isUniqueConstraintViolation())

        assertEquals(1, getStudents().size)
    }

    @Test
    fun `creating two people with same valpas link fails`() {
        createStudent(minimalStudentAndCaseInput(valpasLink = "http://valpas.fi/123"))
        val e =
            assertThrows<UnableToExecuteStatementException> {
                createStudent(minimalStudentAndCaseInput(valpasLink = "http://valpas.fi/123"))
            }
        assertTrue(e.isUniqueConstraintViolation())

        assertEquals(1, getStudents().size)
    }

    @Test
    fun `duplicate ssn is detected`() {
        db.transaction { tx ->
            val student = DevStudent(createdBy = testUser.id, created = now, ssn = "170108A927R")
            tx.insert(student)
            tx.insert(
                DevStudentCase(studentId = student.id, createdBy = testUser.id, created = now)
            )
        }

        val duplicateStudents =
            getDuplicateStudents(
                DuplicateStudentCheckInput(
                    ssn = "170108A927R",
                    valpasLink = "",
                    valpasOppijaOid = "",
                    firstName = "",
                    lastName = "",
                )
            )
        assertEquals(1, duplicateStudents.size)
        duplicateStudents.first().let { duplicate ->
            assertTrue(duplicate.matchingSsn)
            assertFalse(duplicate.matchingValpasLink)
            assertFalse(duplicate.matchingOppijaOid)
            assertFalse(duplicate.matchingName)
        }
    }

    @Test
    fun `duplicate valpasLink is detected`() {
        db.transaction { tx ->
            val student =
                DevStudent(
                    createdBy = testUser.id,
                    created = now,
                    valpasLink = "https://valpas.fi/123",
                )
            tx.insert(student)
            tx.insert(
                DevStudentCase(studentId = student.id, createdBy = testUser.id, created = now)
            )
        }

        val duplicateStudents =
            getDuplicateStudents(
                DuplicateStudentCheckInput(
                    ssn = "",
                    valpasLink = "https://valpas.fi/123",
                    valpasOppijaOid = "",
                    firstName = "",
                    lastName = "",
                )
            )
        assertEquals(1, duplicateStudents.size)
        duplicateStudents.first().let { duplicate ->
            assertFalse(duplicate.matchingSsn)
            assertTrue(duplicate.matchingValpasLink)
            assertFalse(duplicate.matchingOppijaOid)
            assertFalse(duplicate.matchingName)
        }
    }

    @Test
    fun `duplicate valpasOppijaOid is detected`() {
        db.transaction { tx ->
            val student =
                DevStudent(
                    createdBy = testUser.id,
                    created = now,
                    valpasOppijaOid = "1.2.246.562.24.10000000001",
                )
            tx.insert(student)
            tx.insert(
                DevStudentCase(studentId = student.id, createdBy = testUser.id, created = now)
            )
        }

        val duplicateStudents =
            getDuplicateStudents(
                DuplicateStudentCheckInput(
                    ssn = "",
                    valpasLink = "",
                    valpasOppijaOid = "1.2.246.562.24.10000000001",
                    firstName = "",
                    lastName = "",
                )
            )
        assertEquals(1, duplicateStudents.size)
        duplicateStudents.first().let { duplicate ->
            assertFalse(duplicate.matchingSsn)
            assertFalse(duplicate.matchingValpasLink)
            assertTrue(duplicate.matchingOppijaOid)
            assertFalse(duplicate.matchingName)
        }
    }

    @Test
    fun `duplicate name is detected`() {
        db.transaction { tx ->
            val student =
                DevStudent(
                    createdBy = testUser.id,
                    created = now,
                    firstName = "Tupu",
                    lastName = "Ankka",
                )
            tx.insert(student)
            tx.insert(
                DevStudentCase(studentId = student.id, createdBy = testUser.id, created = now)
            )
        }

        val duplicateStudents =
            getDuplicateStudents(
                DuplicateStudentCheckInput(
                    ssn = "",
                    valpasLink = "",
                    valpasOppijaOid = "",
                    firstName = "Tupu",
                    lastName = "Ankka",
                )
            )
        assertEquals(1, duplicateStudents.size)
        duplicateStudents.first().let { duplicate ->
            assertFalse(duplicate.matchingSsn)
            assertFalse(duplicate.matchingValpasLink)
            assertFalse(duplicate.matchingOppijaOid)
            assertTrue(duplicate.matchingName)
        }
    }

    @Test
    fun `duplicate name is ignored if both students have ssn`() {
        db.transaction { tx ->
            val student =
                DevStudent(
                    createdBy = testUser.id,
                    created = now,
                    ssn = "170108A927R",
                    firstName = "Tupu",
                    lastName = "Ankka",
                )
            tx.insert(student)
            tx.insert(
                DevStudentCase(studentId = student.id, createdBy = testUser.id, created = now)
            )
        }

        val duplicateStudents =
            getDuplicateStudents(
                DuplicateStudentCheckInput(
                    ssn = "100507A967F",
                    valpasLink = "",
                    valpasOppijaOid = "",
                    firstName = "Tupu",
                    lastName = "Ankka",
                )
            )
        assertEquals(0, duplicateStudents.size)
    }

    @Test
    fun `deleting student fails when it has cases`() {
        val student = DevStudent(createdBy = testUser.id, created = now)
        db.transaction { tx ->
            tx.insert(student)
            tx.insert(
                DevStudentCase(studentId = student.id, createdBy = testUser.id, created = now)
            )
        }
        assertThrows<UnableToExecuteStatementException> { deleteStudent(student.id) }
    }

    @Test
    fun `deleting student after deleting its cases`() {
        val student = DevStudent(createdBy = testUser.id, created = now)
        val studentCase =
            DevStudentCase(studentId = student.id, createdBy = testUser.id, created = now)
        db.transaction { tx ->
            tx.insert(student)
            tx.insert(studentCase)
        }
        deleteStudentCase(student.id, studentCase.id)

        deleteStudent(student.id)

        assertEquals(0, getStudents().size)
        assertThrows<NotFound> { getStudent(student.id) }
    }

    @Test
    fun `deleting old students`() {
        val oldStudent =
            DevStudent(
                createdBy = testUser.id,
                created = now,
                dateOfBirth = clock.today().minusYears(21).minusDays(1),
            )
        val youngStudent =
            DevStudent(
                createdBy = testUser.id,
                created = now,
                dateOfBirth = clock.today().minusYears(21).plusDays(1),
            )
        val oldStudentCase =
            DevStudentCase(studentId = oldStudent.id, createdBy = testUser.id, created = now)
        db.transaction { tx ->
            tx.insert(oldStudent)
            tx.insert(youngStudent)
            tx.insert(oldStudentCase)
            tx.insert(
                DevStudentCase(studentId = youngStudent.id, createdBy = testUser.id, created = now)
            )
        }
        createCaseEvent(oldStudentCase.id, CaseEventInput(clock.today(), CaseEventType.NOTE, "foo"))

        deleteOldStudents()

        val students = getStudents()
        assertEquals(1, students.size)
        assertEquals(youngStudent.id, students.first().id)
        assertThrows<NotFound> { getStudent(oldStudent.id) }
    }

    private fun minimalStudentAndCaseInput(
        ssn: String = "",
        valpasLink: String = "",
    ): AppController.StudentAndCaseInput =
        AppController.StudentAndCaseInput(
            student =
                StudentInput(
                    valpasLink = valpasLink,
                    valpasOppijaOid = null,
                    ssn = ssn,
                    firstName = "Testi",
                    lastName = "Testilä",
                    language = "",
                    dateOfBirth = LocalDate.now().minusYears(16),
                    phone = "",
                    email = "",
                    gender = null,
                    address = "",
                    municipalityInFinland = true,
                    guardianInfo = "",
                    supportContactsInfo = "",
                    partnerOrganisations = emptySet(),
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
                    notInSchoolReason = null,
                ),
        )
}
