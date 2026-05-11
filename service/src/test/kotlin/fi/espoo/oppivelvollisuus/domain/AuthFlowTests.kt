// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.FullApplicationTest
import fi.espoo.oppivelvollisuus.TestHttpClient
import fi.espoo.oppivelvollisuus.makeTestToken
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import testUser
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AuthFlowTests : FullApplicationTest() {
    @LocalServerPort
    var port: Int = 0

    private val systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")

    private val http by lazy { TestHttpClient(port) }

    @Test
    fun `health endpoint bypasses auth`() {
        // The auth filter skips /health (no 401/403), but there is no handler registered for this
        // path so Spring returns 404. The important thing is: no token is required (not 401).
        val status = http.get("/health").statusCode
        assertNotEquals(401, status, "Expected /health to bypass auth (no token required)")
        assertNotEquals(403, status, "Expected /health to bypass auth (no token required)")
    }

    @Test
    fun `actuator health endpoint bypasses auth`() {
        assertEquals(200, http.get("/actuator/health").statusCode)
    }

    @Test
    fun `protected endpoint without token returns 401`() {
        assertEquals(401, http.get("/employees").statusCode)
    }

    @Test
    fun `protected endpoint with invalid token returns 401`() {
        assertEquals(401, http.get("/employees", "this-is-not-a-valid-jwt").statusCode)
    }

    @Test
    fun `protected endpoint with valid token reaches handler`() {
        val token = makeTestToken(testUser.id.toString())
        val status = http.get("/employees", token).statusCode
        // 200 OK means the handler was reached (returns empty list); anything but 401/403 is fine
        assertEquals(200, status)
    }

    @Test
    fun `system endpoint with non-system user returns 403`() {
        val token = makeTestToken(testUser.id.toString())
        assertEquals(403, http.get("/system/users/${UUID.randomUUID()}", token).statusCode)
    }

    @Test
    fun `system endpoint with system user is allowed`() {
        val token = makeTestToken(systemUserId.toString())
        val status = http.get("/system/users/${UUID.randomUUID()}", token).statusCode
        // 200 (null body for unknown id) or 404 — anything but 403 is acceptable
        assertNotEquals(403, status, "Expected system user to be allowed on /system/* but got $status")
    }
}
