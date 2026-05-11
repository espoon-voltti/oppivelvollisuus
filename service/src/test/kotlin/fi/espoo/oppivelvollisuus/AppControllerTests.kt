// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.domain.AppController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import testUser
import kotlin.test.assertEquals

class AppControllerTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: AppController

    @Test
    fun `get employee users returns the test user`() {
        val employees = controller.getEmployeeUsers(testUser)
        assertEquals(1, employees.size)
        employees.first().let { user ->
            assertEquals(testUser.id, user.id)
        }
    }
}
