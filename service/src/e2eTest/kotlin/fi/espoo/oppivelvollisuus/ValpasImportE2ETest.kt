// SPDX-FileCopyrightText: 2026-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.domain.CaseStatus
import fi.espoo.oppivelvollisuus.domain.ValpasNotifier
import fi.espoo.oppivelvollisuus.pages.LoginPage
import fi.espoo.oppivelvollisuus.pages.StudentPage
import fi.espoo.oppivelvollisuus.shared.dev.DevStudent
import fi.espoo.oppivelvollisuus.shared.dev.DevStudentCase
import fi.espoo.oppivelvollisuus.shared.dev.DevUser
import fi.espoo.oppivelvollisuus.shared.dev.insert
import java.util.UUID
import org.junit.jupiter.api.Test

class ValpasImportE2ETest : PlaywrightTest() {

    @Test
    fun `approve an IMPORTED_FROM_VALPAS case end-to-end`() {
        val page = getPageWithDefaultOptions()
        doLogin(page)

        val (studentId, importedCaseId) = seedImportedFromValpasCase()
        page.navigate("$baseUrl/oppivelvolliset/$studentId")

        val studentPage = StudentPage(page)
        assertThat(studentPage.studentName).isVisible()

        val caseRow = studentPage.caseRow(importedCaseId)
        caseRow.assertStatus(CaseStatus.IMPORTED_FROM_VALPAS)

        // The approve button must be visible before filling in required fields.
        // At this point sourceValpas is missing so the approve button is disabled.
        assertThat(caseRow.approveValpasButton).isVisible()
        assertThat(caseRow.approveValpasButton).isDisabled()

        // Fill in the missing "Ilmoittanut taho" (sourceValpas) via the edit form.
        caseRow.editButton.click()
        caseRow.sourceValpasSelect.selectOption(ValpasNotifier.PERUSOPETUS.name)
        caseRow.saveCaseEditButton.click()

        // After saving, the approve button should now be enabled.
        assertThat(caseRow.approveValpasButton).isEnabled()
        caseRow.approveValpasButton.click()

        // Case status should transition to TODO.
        caseRow.assertStatus(CaseStatus.TODO)
    }

    @Test
    fun `mark-as-duplicate-of-latest flow`() {
        val page = getPageWithDefaultOptions()
        doLogin(page)

        val seeder = DevUser(lastName = "Seeder")
        val student = DevStudent(createdBy = seeder.id)
        val notificationId = UUID.randomUUID()
        // Active TODO case – no valpas_notification_id
        val activeTodoCase =
            DevStudentCase(
                studentId = student.id,
                createdBy = seeder.id,
                status = CaseStatus.TODO,
                source = CaseSource.VALPAS_AUTOMATIC_CHECK,
            )
        // Imported case with a valpas_notification_id
        val importedCase =
            DevStudentCase(
                studentId = student.id,
                createdBy = seeder.id,
                status = CaseStatus.IMPORTED_FROM_VALPAS,
                source = CaseSource.VALPAS_NOTICE,
                valpasNotificationId = notificationId,
            )
        db.transaction { tx ->
            tx.insert(seeder)
            tx.insert(student)
            tx.insert(activeTodoCase)
            tx.insert(importedCase)
        }

        page.navigate("$baseUrl/oppivelvolliset/${student.id}")

        val studentPage = StudentPage(page)
        assertThat(studentPage.studentName).isVisible()

        // Both cases exist initially.
        assertThat(studentPage.caseRows).hasCount(2)

        val importedCaseRow = studentPage.caseRow(importedCase.id)
        importedCaseRow.assertStatus(CaseStatus.IMPORTED_FROM_VALPAS)

        // "Merkitse duplikaatiksi" button is visible because an active TODO case exists.
        assertThat(importedCaseRow.markAsDuplicateButton).isVisible()

        // Confirm the window.confirm dialog.
        page.acceptNextDialog()
        importedCaseRow.markAsDuplicateButton.click()

        // After the duplicate merge, we are redirected to the same student page and only the
        // active TODO case remains.
        studentPage.assertUrl()
        assertThat(studentPage.caseRows).hasCount(1)
        val remainingCaseRow = studentPage.caseRow(activeTodoCase.id)
        remainingCaseRow.assertStatus(CaseStatus.TODO)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun seedImportedFromValpasCase(): Pair<StudentId, StudentCaseId> {
        val seeder = DevUser(lastName = "Seeder")
        // Match what the integration would produce: ssn populated from hetu.
        val student = DevStudent(createdBy = seeder.id, ssn = "150610A123B")
        val importedCase =
            DevStudentCase(
                studentId = student.id,
                createdBy = seeder.id,
                status = CaseStatus.IMPORTED_FROM_VALPAS,
                source = CaseSource.VALPAS_NOTICE,
                // sourceValpas intentionally null so required fields are initially missing
                valpasNotificationId = UUID.randomUUID(),
            )
        db.transaction { tx ->
            tx.insert(seeder)
            tx.insert(student)
            tx.insert(importedCase)
        }
        return student.id to importedCase.id
    }

    private fun doLogin(page: Page) {
        page.navigate("$baseUrl/kirjaudu")
        val loginPage = LoginPage(page)
        loginPage.assertUrl()
        loginPage.login()
    }
}
