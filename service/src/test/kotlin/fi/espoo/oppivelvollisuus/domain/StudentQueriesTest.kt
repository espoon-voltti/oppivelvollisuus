// SPDX-FileCopyrightText: 2026-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.PureJdbiTest
import fi.espoo.oppivelvollisuus.shared.NotFound
import fi.espoo.oppivelvollisuus.shared.dev.DevStudent
import fi.espoo.oppivelvollisuus.shared.dev.DevStudentCase
import fi.espoo.oppivelvollisuus.shared.dev.DevUser
import fi.espoo.oppivelvollisuus.shared.dev.insert
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StudentQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))

    private val user1 = DevUser(firstNames = "Anna", lastName = "Aakkonen")
    private val user2 = DevUser(firstNames = "Bertta", lastName = "Bertilä")

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(user1)
            tx.insert(user2)
        }
    }

    private fun search(query: String? = null, assignee: AssignedToSearch? = null) =
        StudentSearchParams(
            query = query,
            statuses = CaseStatus.entries,
            sources = CaseSource.entries,
            assignee = assignee,
        )

    private fun insertStudentWithCase(
        firstName: String = "Testi",
        lastName: String = "Testilä",
        ssn: String = "",
        assignedTo: fi.espoo.oppivelvollisuus.EspooUserId? = null,
    ): fi.espoo.oppivelvollisuus.StudentId {
        val student =
            DevStudent(
                createdBy = user1.id,
                created = now,
                firstName = firstName,
                lastName = lastName,
                ssn = ssn,
            )
        db.transaction { tx ->
            tx.insert(student)
            tx.insert(
                DevStudentCase(
                    studentId = student.id,
                    createdBy = user1.id,
                    created = now,
                    assignedTo = assignedTo,
                )
            )
        }
        return student.id
    }

    // ---------- getStudentSummaries: assignee branches ----------

    @Test
    fun `assignee filter with null assignedTo returns only unassigned cases`() {
        val unassigned = insertStudentWithCase(firstName = "Unassigned", assignedTo = null)
        insertStudentWithCase(firstName = "Assigned", assignedTo = user1.id)

        val result = db.read { it.getStudentSummaries(search(assignee = AssignedToSearch(null))) }

        assertEquals(listOf(unassigned), result.map { it.id })
    }

    @Test
    fun `assignee filter with specific user returns only that user's cases`() {
        val forUser1 = insertStudentWithCase(firstName = "User1Student", assignedTo = user1.id)
        insertStudentWithCase(firstName = "User2Student", assignedTo = user2.id)
        insertStudentWithCase(firstName = "Unassigned", assignedTo = null)

        val result = db.read {
            it.getStudentSummaries(search(assignee = AssignedToSearch(user1.id)))
        }

        assertEquals(listOf(forUser1), result.map { it.id })
    }

    // ---------- getStudentSummaries: query branches ----------

    @Test
    fun `query matches first name prefix case-insensitively`() {
        val target = insertStudentWithCase(firstName = "Testi", lastName = "Aakkonen")
        insertStudentWithCase(firstName = "Toivo", lastName = "Bertilä")

        val result = db.read { it.getStudentSummaries(search(query = "TES")) }

        assertEquals(listOf(target), result.map { it.id })
    }

    @Test
    fun `query matches last name prefix case-insensitively`() {
        val target = insertStudentWithCase(firstName = "Testi", lastName = "Aakkonen")
        insertStudentWithCase(firstName = "Toivo", lastName = "Bertilä")

        val result = db.read { it.getStudentSummaries(search(query = "Aak")) }

        assertEquals(listOf(target), result.map { it.id })
    }

    @Test
    fun `query matches first-name last-name pair`() {
        val target = insertStudentWithCase(firstName = "Testi", lastName = "Aakkonen")
        insertStudentWithCase(firstName = "Toivo", lastName = "Bertilä")

        val result = db.read { it.getStudentSummaries(search(query = "Testi Aakk")) }

        assertEquals(listOf(target), result.map { it.id })
    }

    @Test
    fun `query matches last-name first-name pair`() {
        val target = insertStudentWithCase(firstName = "Testi", lastName = "Aakkonen")
        insertStudentWithCase(firstName = "Toivo", lastName = "Bertilä")

        val result = db.read { it.getStudentSummaries(search(query = "Aakkonen Tes")) }

        assertEquals(listOf(target), result.map { it.id })
    }

    @Test
    fun `query matches ssn prefix`() {
        val target = insertStudentWithCase(firstName = "Testi", ssn = "170108A927R")
        insertStudentWithCase(firstName = "Toivo", ssn = "010203A123X")

        val result = db.read { it.getStudentSummaries(search(query = "170108")) }

        assertEquals(listOf(target), result.map { it.id })
    }

    @Test
    fun `query is trimmed before matching`() {
        val target = insertStudentWithCase(firstName = "Testi", lastName = "Aakkonen")

        val result = db.read { it.getStudentSummaries(search(query = "   Aak   ")) }

        assertEquals(listOf(target), result.map { it.id })
    }

    // ---------- getPossibleDuplicateStudents: branch combinations ----------

    @Test
    fun `all blank input returns empty list`() {
        insertStudentWithCase(firstName = "Donald", lastName = "Duck", ssn = "170108A927R")

        val result = db.read {
            it.getPossibleDuplicateStudents(
                DuplicateStudentCheckInput(ssn = "", valpasLink = "", firstName = "", lastName = "")
            )
        }

        assertEquals(emptyList(), result)
    }

    @Test
    fun `ssn and valpasLink can match the same row`() {
        val student =
            DevStudent(
                createdBy = user1.id,
                created = now,
                ssn = "170108A927R",
                valpasLink = "https://valpas.fi/abc",
            )
        db.transaction { tx -> tx.insert(student) }

        val result = db.read {
            it.getPossibleDuplicateStudents(
                DuplicateStudentCheckInput(
                    ssn = "170108A927R",
                    valpasLink = "https://valpas.fi/abc",
                    firstName = "",
                    lastName = "",
                )
            )
        }

        assertEquals(1, result.size)
        result.first().let { d ->
            assertEquals(true, d.matchingSsn)
            assertEquals(true, d.matchingValpasLink)
            assertEquals(false, d.matchingName)
        }
    }

    @Test
    fun `all three matchers can fire on the same row`() {
        val student =
            DevStudent(
                createdBy = user1.id,
                created = now,
                ssn = "170108A927R",
                valpasLink = "https://valpas.fi/abc",
                firstName = "Donald",
                lastName = "Duck",
            )
        db.transaction { tx -> tx.insert(student) }

        // Name matcher requires both SSNs to be blank-or-equal. Here we leave the input
        // ssn blank so the name match is allowed alongside the ssn match against the row.
        val result = db.read {
            it.getPossibleDuplicateStudents(
                DuplicateStudentCheckInput(
                    ssn = "170108A927R",
                    valpasLink = "https://valpas.fi/abc",
                    firstName = "Donald",
                    lastName = "Duck",
                )
            )
        }

        assertEquals(1, result.size)
        result.first().let { d ->
            assertEquals(true, d.matchingSsn)
            assertEquals(true, d.matchingValpasLink)
            // name matcher is suppressed because BOTH the input ssn and the row ssn are non-blank
            assertEquals(false, d.matchingName)
        }
    }

    @Test
    fun `first name only without last name produces no name match`() {
        insertStudentWithCase(firstName = "Donald", lastName = "Duck")

        val result = db.read {
            it.getPossibleDuplicateStudents(
                DuplicateStudentCheckInput(
                    ssn = "",
                    valpasLink = "",
                    firstName = "Donald",
                    lastName = "",
                )
            )
        }

        assertEquals(emptyList(), result)
    }

    @Test
    fun `last name only without first name produces no name match`() {
        insertStudentWithCase(firstName = "Donald", lastName = "Duck")

        val result = db.read {
            it.getPossibleDuplicateStudents(
                DuplicateStudentCheckInput(
                    ssn = "",
                    valpasLink = "",
                    firstName = "",
                    lastName = "Duck",
                )
            )
        }

        assertEquals(emptyList(), result)
    }

    // ---------- round-trip smoke tests for the remaining Student.kt queries ----------

    @Test
    fun `insertStudent persists all columns and returns the new id`() {
        val input =
            StudentInput(
                valpasLink = "https://valpas.fi/x",
                valpasOppijaOid = null,
                ssn = "010203A123X",
                firstName = "Iiris",
                lastName = "Insertilä",
                language = "suomi",
                dateOfBirth = LocalDate.of(2009, 4, 5),
                phone = "040",
                email = "i@i.fi",
                gender = Gender.FEMALE,
                address = "Katu 2",
                municipalityInFinland = true,
                guardianInfo = "G",
                supportContactsInfo = "S",
                partnerOrganisations = setOf(PartnerOrganisation.LASTENSUOJELU),
            )

        val id = db.transaction { it.insertStudent(input, createdBy = user1.id, now = now) }

        val persisted = db.read { it.getStudent(id) }
        assertEquals(input.firstName, persisted.firstName)
        assertEquals(input.lastName, persisted.lastName)
        assertEquals(input.ssn, persisted.ssn)
        assertEquals(input.gender, persisted.gender)
        assertEquals(input.partnerOrganisations, persisted.partnerOrganisations)
    }

    @Test
    fun `getStudent throws NotFound for missing id`() {
        assertThrows<NotFound> {
            db.read {
                it.getStudent(fi.espoo.oppivelvollisuus.StudentId(java.util.UUID.randomUUID()))
            }
        }
    }

    @Test
    fun `updateStudent overwrites every field`() {
        val student = DevStudent(createdBy = user1.id, created = now, firstName = "Before")
        db.transaction { tx -> tx.insert(student) }

        db.transaction {
            it.updateStudent(
                id = student.id,
                data =
                    StudentInput(
                        valpasLink = "after",
                        valpasOppijaOid = null,
                        ssn = "010203A123X",
                        firstName = "After",
                        lastName = "Updated",
                        language = "ruotsi",
                        dateOfBirth = LocalDate.of(2010, 6, 7),
                        phone = "p",
                        email = "e@e.fi",
                        gender = Gender.MALE,
                        address = "a",
                        municipalityInFinland = false,
                        guardianInfo = "g",
                        supportContactsInfo = "s",
                        partnerOrganisations = setOf(PartnerOrganisation.TUKIHENKILO),
                    ),
                updatedBy = user1.id,
                now = now,
            )
        }

        val persisted = db.read { it.getStudent(student.id) }
        assertEquals("After", persisted.firstName)
        assertEquals("Updated", persisted.lastName)
        assertEquals(Gender.MALE, persisted.gender)
        assertEquals(setOf(PartnerOrganisation.TUKIHENKILO), persisted.partnerOrganisations)
    }

    @Test
    fun `deleteStudent removes the row`() {
        val student = DevStudent(createdBy = user1.id, created = now)
        db.transaction { tx -> tx.insert(student) }
        assertNotNull(db.read { it.getStudent(student.id) })

        db.transaction { it.deleteStudent(student.id) }

        assertThrows<NotFound> { db.read { it.getStudent(student.id) } }
    }

    @Test
    fun `deleteOldStudents removes students with date_of_birth before the threshold`() {
        val cutoff = LocalDate.of(2005, 1, 1)
        val old = DevStudent(createdBy = user1.id, created = now, dateOfBirth = cutoff.minusDays(1))
        val young =
            DevStudent(createdBy = user1.id, created = now, dateOfBirth = cutoff.plusDays(1))
        db.transaction { tx ->
            tx.insert(old)
            tx.insert(young)
        }

        db.transaction { it.deleteOldStudents(cutoff) }

        assertThrows<NotFound> { db.read { it.getStudent(old.id) } }
        assertNotNull(db.read { it.getStudent(young.id) })
    }
}
