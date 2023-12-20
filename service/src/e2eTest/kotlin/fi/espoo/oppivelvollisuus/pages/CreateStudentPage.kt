package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.BASE_URL
import fi.espoo.oppivelvollisuus.dataQa

class CreateStudentPage(private val page: Page) {
    val saveButton = page.locator(dataQa("save-button"))
    val lastNameInput = page.locator(dataQa("last-name-input"))
    val firstNameInput = page.locator(dataQa("first-name-input"))
    val sourceSelect = page.locator(dataQa("source-select"))

    fun assertUrl() {
        assertThat(page).hasURL("$BASE_URL/oppivelvolliset/uusi")
    }
}
