// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.baseUrl
import fi.espoo.oppivelvollisuus.dataQa

class StudentsSearchPage(private val page: Page) {
    val createStudentButton = page.locator(dataQa("create-student-button"))

    fun assertUrl() {
        assertThat(page).hasURL("$baseUrl/oppivelvolliset")
    }
}
