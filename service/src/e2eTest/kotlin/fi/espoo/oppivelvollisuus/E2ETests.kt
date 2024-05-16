// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.pages.CreateStudentPage
import fi.espoo.oppivelvollisuus.pages.LoginPage
import fi.espoo.oppivelvollisuus.pages.StudentPage
import fi.espoo.oppivelvollisuus.pages.StudentsSearchPage
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

    private fun doLogin(page: Page) {
        page.navigate("$baseUrl/kirjaudu")
        val loginPage = LoginPage(page)
        loginPage.assertUrl()
        loginPage.login()
    }
}
