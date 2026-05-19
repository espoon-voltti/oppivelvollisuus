// SPDX-FileCopyrightText: 2026-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.microsoft.playwright.Page
import kotlin.io.path.Path
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher

class PlaywrightTestWatcher : TestWatcher {
    override fun testFailed(context: ExtensionContext, cause: Throwable?) {
        super.testFailed(context, cause)
        val testName = context.displayName.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val timestamp = System.currentTimeMillis()

        (context.requiredTestInstance as? PlaywrightTest)?.also { testInstance ->
            testInstance.browser.contexts().forEach { browserContext ->
                browserContext.pages().forEachIndexed { index, page ->
                    val screenshotOptions = Page.ScreenshotOptions()
                    val path = Path("screenshots/${testName}_page${index}_$timestamp.png")
                    screenshotOptions.setPath(path)
                    page.screenshot(screenshotOptions)
                }
                browserContext.close()
            }
        }
    }

    override fun testSuccessful(context: ExtensionContext) {
        super.testSuccessful(context)
        (context.requiredTestInstance as? PlaywrightTest)?.also { testInstance ->
            testInstance.browser.contexts().forEach { browserContext -> browserContext.close() }
        }
    }
}
