// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.AdUser
import fi.espoo.oppivelvollisuus.EspooUserId
import fi.espoo.oppivelvollisuus.FullApplicationTest
import fi.espoo.oppivelvollisuus.SystemController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SystemControllerTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var systemController: SystemController

    @Test
    fun `user login upserts user and returns app user`() {
        val adUser =
            AdUser(
                externalId = "ext-123",
                firstName = "Matti",
                lastName = "Meikäläinen",
                email = "matti@example.com"
            )

        val result = systemController.userLogin(adUser, dbInstance())

        assertEquals("ext-123", result.externalId)
        assertEquals("Matti", result.firstName)
        assertEquals("Meikäläinen", result.lastName)
        assertEquals("matti@example.com", result.email)

        // Second login with same externalId should update and return the same user id
        val updatedAdUser = adUser.copy(firstName = "Matias")
        val result2 = systemController.userLogin(updatedAdUser, dbInstance())
        assertEquals(result.id, result2.id)
        assertEquals("Matias", result2.firstName)
    }

    @Test
    fun `get user returns existing user`() {
        val adUser =
            AdUser(
                externalId = "ext-456",
                firstName = "Liisa",
                lastName = "Virtanen",
                email = null
            )
        val created = systemController.userLogin(adUser, dbInstance())

        val found = systemController.getUser(created.id, dbInstance())
        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("Liisa", found.firstName)
    }

    @Test
    fun `get user returns null for unknown id`() {
        val result = systemController.getUser(EspooUserId(UUID.randomUUID()), dbInstance())
        assertNull(result)
    }
}
