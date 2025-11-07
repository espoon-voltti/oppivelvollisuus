// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.baseUrl
import fi.espoo.oppivelvollisuus.dataQa
import kotlin.test.assertEquals

class StudentsSearchPage(
    private val page: Page
) {
    val createStudentButton = page.locator(dataQa("create-student-button"))
    val employeeSelect = page.locator(dataQa("employee-select"))

    fun assertUrl() {
        assertThat(page).hasURL("$baseUrl/oppivelvolliset")
    }

    fun assertEmployeeSelectOptions(expected: List<String>) {
        assertEquals(expected.joinToString(""), employeeSelect.allTextContents().joinToString())
    }
}
