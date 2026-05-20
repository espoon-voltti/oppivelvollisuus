// SPDX-FileCopyrightText: 2025-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import fi.espoo.oppivelvollisuus.PureJdbiTest
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class ValpasQueryRunQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDateTime.of(2026, 5, 20, 3, 0))

    @Test
    fun `round-trip through every state transition`() {
        val id = db.transaction { tx ->
            tx.insertValpasQueryRun(externalQueryId = "query-abc", now = now)
        }

        val afterInsert = db.read { it.getMostRecentValpasQueryRun() }
        assertEquals(ValpasQueryRunState.STARTED, afterInsert?.state)
        assertEquals("query-abc", afterInsert?.externalQueryId)

        db.transaction { tx ->
            tx.markValpasQueryRunFilesReady(
                id = id,
                fileUrls = listOf("https://example.com/file1.csv", "https://example.com/file2.csv"),
                now = now,
            )
        }

        val afterFilesReady = db.read { it.getMostRecentValpasQueryRun() }
        assertEquals(ValpasQueryRunState.FILES_READY, afterFilesReady?.state)
        assertEquals(
            listOf("https://example.com/file1.csv", "https://example.com/file2.csv"),
            afterFilesReady?.fileUrls,
        )

        db.transaction { tx -> tx.markValpasQueryRunCompleted(id = id, now = now) }

        val afterCompleted = db.read { it.getMostRecentValpasQueryRun() }
        assertEquals(ValpasQueryRunState.COMPLETED, afterCompleted?.state)
    }

    @Test
    fun `failed state transition`() {
        val id = db.transaction { tx ->
            tx.insertValpasQueryRun(externalQueryId = "query-fail", now = now)
        }

        db.transaction { tx -> tx.markValpasQueryRunFailed(id = id, now = now) }

        val result = db.read { it.getMostRecentValpasQueryRun() }
        assertEquals(ValpasQueryRunState.FAILED, result?.state)
    }

    @Test
    fun `cancelled state transition`() {
        val id = db.transaction { tx ->
            tx.insertValpasQueryRun(externalQueryId = "query-cancel", now = now)
        }

        db.transaction { tx -> tx.markValpasQueryRunCancelled(id = id, now = now) }

        val result = db.read { it.getMostRecentValpasQueryRun() }
        assertEquals(ValpasQueryRunState.CANCELLED, result?.state)
    }

    @Test
    fun `getMostRecentValpasQueryRun returns null when table is empty`() {
        val result = db.read { it.getMostRecentValpasQueryRun() }
        assertNull(result)
    }
}
