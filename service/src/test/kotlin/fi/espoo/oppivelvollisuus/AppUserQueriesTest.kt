// SPDX-FileCopyrightText: 2026-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.shared.auth.AdUser
import fi.espoo.oppivelvollisuus.shared.dev.DevUser
import fi.espoo.oppivelvollisuus.shared.dev.insert
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class AppUserQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))

    @Test
    fun `upsertAppUserFromAd inserts a new user`() {
        val adUser =
            AdUser(
                externalId = "ext-1",
                firstName = "Iida",
                lastName = "Insertilä",
                email = "i@i.fi",
            )

        val inserted = db.transaction { it.upsertAppUserFromAd(adUser, now) }

        assertEquals("ext-1", inserted.externalId)
        assertEquals("Iida Insertilä", inserted.firstName + " " + inserted.lastName)
        assertEquals("i@i.fi", inserted.email)
        assertEquals(true, inserted.isActive)

        val readBack = db.read { it.getAppUser(inserted.id) }
        assertEquals(inserted, readBack)
    }

    @Test
    fun `upsertAppUserFromAd updates an existing user on conflict`() {
        val first = db.transaction {
            it.upsertAppUserFromAd(
                AdUser(externalId = "ext-1", firstName = "Iida", lastName = "Vanha", email = null),
                now,
            )
        }

        val updated = db.transaction {
            it.upsertAppUserFromAd(
                AdUser(
                    externalId = "ext-1",
                    firstName = "Iida",
                    lastName = "Uusi",
                    email = "u@u.fi",
                ),
                now,
            )
        }

        assertEquals(first.id, updated.id)
        assertEquals("Uusi", updated.lastName)
        assertEquals("u@u.fi", updated.email)
    }

    @Test
    fun `getActiveAppUsers returns only non-system active users`() {
        val active = DevUser(firstNames = "Aktiivi", lastName = "AK", isActive = true)
        val inactive = DevUser(firstNames = "Pois", lastName = "IK", isActive = false)
        db.transaction { tx ->
            tx.insert(active)
            tx.insert(inactive)
        }

        val users = db.read { it.getActiveAppUsers() }

        assertEquals(listOf(active.id), users.map { it.id })
    }

    @Test
    fun `getAppUser returns null for unknown id`() {
        assertNull(db.read { it.getAppUser(EspooUserId(UUID.randomUUID())) })
    }

    @Test
    fun `getAppUser returns the matching user`() {
        val user = DevUser(firstNames = "Reetta", lastName = "Read")
        db.transaction { tx -> tx.insert(user) }

        val result = db.read { it.getAppUser(user.id) }

        assertEquals(user.id, result?.id)
        assertEquals("Reetta", result?.firstName)
    }
}
