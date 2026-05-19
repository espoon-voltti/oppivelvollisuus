// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.domain.CaseStatus
import fi.espoo.oppivelvollisuus.pages.CreateStudentPage
import fi.espoo.oppivelvollisuus.pages.LoginPage
import fi.espoo.oppivelvollisuus.pages.StudentPage
import fi.espoo.oppivelvollisuus.pages.StudentsSearchPage
import fi.espoo.oppivelvollisuus.shared.dev.DevStudent
import fi.espoo.oppivelvollisuus.shared.dev.DevStudentCase
import fi.espoo.oppivelvollisuus.shared.dev.DevUser
import fi.espoo.oppivelvollisuus.shared.dev.insert
import org.junit.jupiter.api.Test

class E2ETests : PlaywrightTest() {
    @Test
    fun `creating new student`() {
        val page = getPageWithDefaultOptions()
        doLogin(page)

        val studentsSearchPage = StudentsSearchPage(page)
        studentsSearchPage.assertUrl()
        studentsSearchPage.createStudentButton.click()

        val createStudentPage = CreateStudentPage(page)
        createStudentPage.assertUrl()
        assertThat(createStudentPage.saveButton).isDisabled()
        createStudentPage.dateOfBirthInput.fill("07.01.2008")
        createStudentPage.lastNameInput.fill("Ankka")
        createStudentPage.firstNameInput.fill("Tupu")
        createStudentPage.sourceSelect.selectOption(CaseSource.VALPAS_AUTOMATIC_CHECK.name)
        assertThat(createStudentPage.saveButton).not().isDisabled()
        createStudentPage.saveButton.click()

        val studentPage = StudentPage(page)
        studentPage.assertUrl()
        assertThat(studentPage.studentName).containsText("Ankka Tupu")
    }

    @Test
    fun `deactivated employee is not selectable`() {
        val page = getPageWithDefaultOptions()
        doLogin(page)

        val studentsSearchPage = StudentsSearchPage(page)
        studentsSearchPage.assertUrl()
        studentsSearchPage.assertEmployeeSelectOptions(
            listOf("Näytä kaikki", "Ei ohjaajaa", "Sanna Suunnittelija")
        )

        db.transaction { tx ->
            tx.execute {
                sql("UPDATE users SET is_active = false WHERE last_name = 'Suunnittelija'")
            }
        }

        page.reload()
        studentsSearchPage.employeeSelect.click()
        studentsSearchPage.assertEmployeeSelectOptions(listOf("Näytä kaikki", "Ei ohjaajaa"))

        studentsSearchPage.createStudentButton.click()

        val createStudentPage = CreateStudentPage(page)
        createStudentPage.assertUrl()
        createStudentPage.employeeSelect.click()
        createStudentPage.assertEmployeeSelectOptions(listOf("Ei ohjaajaa"))
    }

    @Test
    fun `case event lifecycle - add, edit, delete`() {
        val page = getPageWithDefaultOptions()
        val (studentId, caseId) = seedStudentWithCase(page)
        page.navigate("$baseUrl/oppivelvolliset/$studentId")

        val studentPage = StudentPage(page)
        assertThat(studentPage.studentName).isVisible()
        val caseRow = studentPage.caseRow(caseId)

        caseRow.addEventButton.click()
        caseRow.eventNotesInput.fill("Soitettu huoltajalle")
        caseRow.saveEventButton.click()

        assertThat(caseRow.eventRows).hasCount(1)
        val eventRow = caseRow.firstEventRow()
        assertThat(eventRow.locator).containsText("Soitettu huoltajalle")

        eventRow.editButton.click()
        eventRow.notesInput.fill("Tavoitettu huoltaja")
        eventRow.saveEditButton.click()
        assertThat(eventRow.locator).containsText("Tavoitettu huoltaja")

        page.acceptNextDialog()
        eventRow.deleteButton.click()
        assertThat(caseRow.eventRows).hasCount(0)
    }

    @Test
    fun `edit and delete student and case`() {
        val page = getPageWithDefaultOptions()
        val (studentId, caseId) = seedStudentWithCase(page)
        page.navigate("$baseUrl/oppivelvolliset/$studentId")

        val studentPage = StudentPage(page)
        assertThat(studentPage.studentName).isVisible()

        studentPage.toggleStudentDetails.click()
        studentPage.editStudentButton.click()
        studentPage.ssnInput.fill("010110A123N")
        studentPage.saveStudentButton.click()
        assertThat(studentPage.ssnValue).hasText("010110A123N")

        val caseRow = studentPage.caseRow(caseId)

        caseRow.editButton.click()
        caseRow.saveCaseEditButton.click()
        assertThat(caseRow.editButton).isVisible()

        page.acceptNextDialog()
        caseRow.deleteButton.click()
        assertThat(studentPage.caseRows).hasCount(0)

        page.acceptNextDialog()
        studentPage.deleteStudentButton.click()
        StudentsSearchPage(page).assertUrl()
    }

    @Test
    fun `finish case and add a second one`() {
        val page = getPageWithDefaultOptions()
        val (studentId, caseId) = seedStudentWithCase(page)
        page.navigate("$baseUrl/oppivelvolliset/$studentId")

        val studentPage = StudentPage(page)
        assertThat(studentPage.studentName).isVisible()
        val caseRow = studentPage.caseRow(caseId)

        assertThat(studentPage.addCaseButton).isDisabled()
        caseRow.assertStatus(CaseStatus.TODO)

        caseRow.changeStatusButton.click()
        caseRow.statusSelect.selectOption("FINISHED")
        caseRow.finishedReasonSelect.selectOption("ERRONEOUS_NOTICE")
        caseRow.saveStatusButton.click()
        caseRow.assertStatus(CaseStatus.FINISHED)

        assertThat(studentPage.addCaseButton).isEnabled()
        studentPage.addCaseButton.click()
        page.dataQa("source-select").selectOption(CaseSource.VALPAS_AUTOMATIC_CHECK.name)
        studentPage.saveCaseButton.click()
        assertThat(studentPage.caseRows).hasCount(2)
    }

    private fun seedStudentWithCase(page: Page): Pair<StudentId, StudentCaseId> {
        doLogin(page)
        val seeder = DevUser(lastName = "Seeder")
        val student = DevStudent(createdBy = seeder.id)
        val studentCase = DevStudentCase(studentId = student.id, createdBy = seeder.id)
        db.transaction { tx ->
            tx.insert(seeder)
            tx.insert(student)
            tx.insert(studentCase)
        }
        return student.id to studentCase.id
    }

    private fun doLogin(page: Page) {
        page.navigate("$baseUrl/kirjaudu")
        val loginPage = LoginPage(page)
        loginPage.assertUrl()
        loginPage.login()
    }
}
