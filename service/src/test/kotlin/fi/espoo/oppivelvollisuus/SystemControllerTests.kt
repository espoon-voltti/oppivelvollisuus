// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.common.AdUser
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SystemControllerTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: SystemController

    @Test
    fun `user login upserts user and returns app user`() {
        val adUser =
            AdUser(
                externalId = "ext-123",
                firstName = "Matti",
                lastName = "Meikäläinen",
                email = "matti@example.com"
            )

        val result = controller.userLogin(adUser)

        assertEquals("ext-123", result.externalId)
        assertEquals("Matti", result.firstName)
        assertEquals("Meikäläinen", result.lastName)
        assertEquals("matti@example.com", result.email)

        // Second login with same externalId should update and return the same user id
        val updatedAdUser = adUser.copy(firstName = "Matias")
        val result2 = controller.userLogin(updatedAdUser)
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
        val created = controller.userLogin(adUser)

        val found = controller.getUser(created.id)
        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("Liisa", found.firstName)
    }

    @Test
    fun `get user returns null for unknown id`() {
        val result = controller.getUser(UUID.randomUUID())
        assertNull(result)
    }
}
