// SPDX-FileCopyrightText: 2026-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.dataQa
import fi.espoo.oppivelvollisuus.domain.CaseStatus

class CaseRow(val locator: Locator) {
    val editButton = locator.dataQa("edit-case-button")
    val deleteButton = locator.dataQa("delete-case-button")
    val saveCaseEditButton = locator.dataQa("save-case-edit-button")

    val statusChip = locator.dataQa("case-status")
    val changeStatusButton = locator.dataQa("change-status-button")
    val saveStatusButton = locator.dataQa("save-status-button")
    val statusSelect = locator.dataQa("status-select")
    val finishedReasonSelect = locator.dataQa("finished-reason-select")

    // Valpas import action buttons (visible only on IMPORTED_FROM_VALPAS cases)
    val approveValpasButton = locator.dataQa("approve-valpas-case-button")
    val markAsDuplicateButton = locator.dataQa("mark-as-duplicate-button")

    // Case edit form fields
    val sourceValpasSelect = locator.dataQa("source-valpas-select")

    val addEventButton = locator.dataQa("add-event-button")
    val saveEventButton = locator.dataQa("save-event-button")
    val eventNotesInput = locator.dataQa("event-notes-input")
    val eventRows = locator.locator("[data-qa^='event-row-']")

    fun firstEventRow() = EventRow(eventRows.first())

    fun assertStatus(expected: CaseStatus) {
        val expectedText =
            when (expected) {
                CaseStatus.IMPORTED_FROM_VALPAS -> "Tuotu Valppaasta"
                CaseStatus.TODO -> "Selvittämättä"
                CaseStatus.ON_HOLD -> "Edistynyt - odottaa"
                CaseStatus.FINISHED -> "Ohjaus päättynyt"
            }
        assertThat(statusChip).hasText(expectedText)
    }
}

class EventRow(val locator: Locator) {
    val editButton = locator.dataQa("edit-event-button")
    val deleteButton = locator.dataQa("delete-event-button")
    val saveEditButton = locator.dataQa("save-event-edit-button")
    val notesInput = locator.dataQa("event-notes-input")
}
