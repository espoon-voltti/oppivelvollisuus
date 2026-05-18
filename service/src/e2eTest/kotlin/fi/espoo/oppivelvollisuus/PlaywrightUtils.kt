// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

fun Page.dataQa(value: String): Locator = this.locator("[data-qa='$value']")

fun Locator.dataQa(dataQa: String): Locator = this.locator("[data-qa='$dataQa']")

class Checkbox(locator: Locator) {
    private val input: Locator = locator.locator("input[type='checkbox']")

    fun check() {
        input.check()
    }

    fun uncheck() {
        input.uncheck()
    }

    fun isChecked(): Boolean = input.isChecked

    fun toggle() {
        if (isChecked()) uncheck() else check()
    }

    fun assertChecked() {
        if (!isChecked()) throw AssertionError("Expected checkbox to be checked, but it was not")
    }

    fun assertUnchecked() {
        if (isChecked())
            throw AssertionError("Expected checkbox to be unchecked, but it was checked")
    }
}

class Modal(
    private val locator: Locator,
    private val confirmButtonDataQa: String = "modal-okBtn",
    private val cancelButtonDataQa: String = "modal-cancelBtn",
) {
    private val confirmButton: Locator = locator.dataQa(confirmButtonDataQa)
    private val cancelButton: Locator = locator.dataQa(cancelButtonDataQa)

    fun confirm() {
        confirmButton.click()
    }

    fun cancel() {
        cancelButton.click()
    }
}
