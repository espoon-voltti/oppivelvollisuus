// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.baseUrl
import fi.espoo.oppivelvollisuus.dataQa
import java.util.regex.Pattern

class StudentPage(private val page: Page) {
    val studentName = page.locator(dataQa("student-name"))

    fun assertUrl() {
        assertThat(page).hasURL(Pattern.compile("$baseUrl/oppivelvolliset/[a-f0-9\\-]+"))
    }
}
