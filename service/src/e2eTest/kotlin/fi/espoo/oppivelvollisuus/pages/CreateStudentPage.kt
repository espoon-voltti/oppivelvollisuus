// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.baseUrl
import fi.espoo.oppivelvollisuus.dataQa

class CreateStudentPage(private val page: Page) {
    val saveButton = page.locator(dataQa("save-button"))
    val dateOfBirthInput = page.locator(dataQa("date-of-birth-input"))
    val lastNameInput = page.locator(dataQa("last-name-input"))
    val firstNameInput = page.locator(dataQa("first-name-input"))
    val sourceSelect = page.locator(dataQa("source-select"))

    fun assertUrl() {
        assertThat(page).hasURL("$baseUrl/oppivelvolliset/uusi")
    }
}
