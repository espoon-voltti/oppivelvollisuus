package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.BASE_URL
import fi.espoo.oppivelvollisuus.dataQa

class LoginPage(private val page: Page) {
    val startLoginButton = page.locator(dataQa("start-login"))
    val loggedInUser = page.locator(dataQa("logged-in-user"))

    fun assertUrl() {
        assertThat(page).hasURL("$BASE_URL/kirjaudu")
    }

    fun login() {
        startLoginButton.click()
        page.locator("button").click()
        assertThat(loggedInUser).containsText("Sanna Suunnittelija")
    }
}
