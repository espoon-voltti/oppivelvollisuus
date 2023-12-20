package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.BASE_URL
import fi.espoo.oppivelvollisuus.dataQa

class StudentsSearchPage(private val page: Page) {
    val createStudentButton = page.locator(dataQa("create-student-button"))

    fun assertUrl() {
        assertThat(page).hasURL("$BASE_URL/oppivelvolliset")
    }
}
