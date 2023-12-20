package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.BASE_URL
import fi.espoo.oppivelvollisuus.dataQa
import java.util.regex.Pattern

class StudentPage(private val page: Page) {
    val studentName = page.locator(dataQa("student-name"))

    fun assertUrl() {
        assertThat(page).hasURL(Pattern.compile("$BASE_URL/oppivelvolliset/[a-f0-9\\-]+"))
    }
}
