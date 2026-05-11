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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks in the HTTP status codes and response body shapes for each error scenario.
 * Phase 2 replaces ExceptionHandler; these tests must remain green.
 *
 * Key findings (actual behavior, not spec assumption):
 *
 * - NotFound (thrown in DB layer after handler is entered) → 404, ErrorResponse body
 *   with { errorCode: String|null, timestamp: Long }, Content-Type: application/json
 *
 * - BadRequest (thrown in data class init blocks during Jackson deserialization) →
 *   Spring intercepts as HttpMessageNotReadableException before ExceptionHandler sees it →
 *   RFC 7807 Problem Detail body { status, title, detail, instance },
 *   Content-Type: application/problem+json.
 *   ExceptionHandler.badRequest() is NOT reached for deserialization-time errors.
 *
 * - UniqueConstraintViolation (UnableToExecuteStatementException) → 409, ErrorResponse
 *   with { errorCode: "UniqueConstraintViolation", timestamp: Long }
 *
 * - Unauthorized/Forbidden are returned by the servlet filter (HttpAccessControl),
 *   not ExceptionHandler; status codes are 401 and 403 respectively.
 */
class ErrorMappingTests : FullApplicationTest() {
    @LocalServerPort
    var port: Int = 0

    private val token by lazy { makeTestToken(testUser.id.toString()) }
    private val http by lazy { TestHttpClient(port) }

    // -------------------------------------------------------------------------
    // NotFound -> 404 with ErrorResponse body
    // Triggered by: GET /students/{unknown-uuid}
    // -------------------------------------------------------------------------

    @Test
    fun `GET unknown student returns 404`() {
        val result = http.get("/students/${UUID.randomUUID()}", token)

        assertEquals(404, result.statusCode)
    }

    @Test
    fun `404 response body has timestamp field (ErrorResponse shape)`() {
        val result = http.get("/students/${UUID.randomUUID()}", token)

        assertEquals(404, result.statusCode)
        // ErrorResponse always has a "timestamp" field even when errorCode is null
        assertTrue(result.body.contains("timestamp"), "Response body must contain 'timestamp': ${result.body}")
        // The response body is a JSON object (not empty, not plain text)
        assertTrue(result.body.trimStart().startsWith("{"), "Response body must be a JSON object: ${result.body}")
    }

    @Test
    fun `404 response Content-Type is application-json`() {
        val result = http.get("/students/${UUID.randomUUID()}", token)

        assertEquals(404, result.statusCode)
        val contentType = checkNotNull(result.contentType) { "Content-Type must be set" }
        assertTrue(
            contentType.contains("application/json", ignoreCase = true),
            "Content-Type must be application/json, was: $contentType"
        )
    }

    // -------------------------------------------------------------------------
    // BadRequest -> 400
    //
    // All BadRequest throws live in data class init{} blocks (StudentCaseInput,
    // CaseStatusInput, FinishedInfo). When these init blocks throw during Jackson
    // deserialization of the request body, Spring wraps the exception in
    // HttpMessageNotReadableException and ResponseEntityExceptionHandler returns
    // a RFC 7807 Problem Detail response — ExceptionHandler.badRequest() is NOT
    // invoked for deserialization-time BadRequest.
    //
    // Actual response: 400, Content-Type: application/problem+json,
    // body: { "status": 400, "title": "Bad Request", "detail": "...", "instance": "..." }
    //
    // This is the behavior Phase 2 must preserve.
    // -------------------------------------------------------------------------

    private val badRequestBody =
        """
        {
          "student": {
            "valpasLink": "",
            "ssn": "",
            "firstName": "Testi",
            "lastName": "Testilä",
            "language": "",
            "dateOfBirth": "2008-01-17",
            "phone": "",
            "email": "",
            "gender": null,
            "address": "",
            "municipalityInFinland": true,
            "guardianInfo": "",
            "supportContactsInfo": "",
            "partnerOrganisations": []
          },
          "studentCase": {
            "openedAt": "2023-12-07",
            "assignedTo": null,
            "source": "VALPAS_NOTICE",
            "sourceValpas": null,
            "sourceOther": null,
            "sourceContact": "",
            "schoolBackground": [],
            "caseBackgroundReasons": [],
            "notInSchoolReason": null
          }
        }
        """.trimIndent()

    @Test
    fun `POST student with mismatched source and sourceValpas returns 400`() {
        // source=VALPAS_NOTICE requires sourceValpas != null.
        // BadRequest is thrown in StudentCaseInput.init during Jackson deserialization,
        // so Spring returns RFC 7807 Problem Detail (not our ErrorResponse).
        val result = http.post("/students", badRequestBody, token)

        assertEquals(400, result.statusCode)
    }

    @Test
    fun `400 via deserialization has RFC 7807 body with status field`() {
        val result = http.post("/students", badRequestBody, token)

        assertEquals(400, result.statusCode)
        // Spring RFC 7807 Problem Detail includes "status" field
        assertTrue(result.body.contains("\"status\""), "Response body must contain 'status': ${result.body}")
        assertTrue(result.body.trimStart().startsWith("{"), "Response body must be a JSON object: ${result.body}")
    }

    @Test
    fun `400 via deserialization has application-problem-json Content-Type`() {
        val result = http.post("/students", badRequestBody, token)

        assertEquals(400, result.statusCode)
        val contentType = checkNotNull(result.contentType) { "Content-Type must be set" }
        assertTrue(
            contentType.contains("application/problem+json", ignoreCase = true),
            "Content-Type must be application/problem+json, was: $contentType"
        )
    }

    // -------------------------------------------------------------------------
    // Conflict (UniqueConstraintViolation) -> 409 with ErrorResponse body
    // Trigger: create two students with the same non-empty SSN.
    // -------------------------------------------------------------------------

    @Test
    fun `duplicate SSN via HTTP returns 409 with UniqueConstraintViolation errorCode`() {
        val studentBody =
            """
            {
              "student": {
                "valpasLink": "",
                "ssn": "170108A927R",
                "firstName": "Testi",
                "lastName": "Testilä",
                "language": "",
                "dateOfBirth": "2008-01-17",
                "phone": "",
                "email": "",
                "gender": null,
                "address": "",
                "municipalityInFinland": true,
                "guardianInfo": "",
                "supportContactsInfo": "",
                "partnerOrganisations": []
              },
              "studentCase": {
                "openedAt": "2023-12-07",
                "assignedTo": null,
                "source": "VALPAS_AUTOMATIC_CHECK",
                "sourceValpas": null,
                "sourceOther": null,
                "sourceContact": "",
                "schoolBackground": [],
                "caseBackgroundReasons": [],
                "notInSchoolReason": null
              }
            }
            """.trimIndent()

        // First student succeeds
        val first = http.post("/students", studentBody, token)
        assertEquals(200, first.statusCode)

        // Second student with same SSN triggers unique constraint
        val second = http.post("/students", studentBody, token)
        assertEquals(409, second.statusCode)
        assertTrue(
            second.body.contains("UniqueConstraintViolation"),
            "errorCode must be 'UniqueConstraintViolation': ${second.body}"
        )
        assertTrue(second.body.contains("timestamp"), "Response must have 'timestamp': ${second.body}")
    }

    @Test
    fun `409 errorCode field is present and non-null for UniqueConstraintViolation`() {
        val studentBody =
            """
            {
              "student": {
                "valpasLink": "https://valpas.example.com/unique-link-for-conflict-test",
                "ssn": "",
                "firstName": "Testi",
                "lastName": "Testilä",
                "language": "",
                "dateOfBirth": "2008-01-17",
                "phone": "",
                "email": "",
                "gender": null,
                "address": "",
                "municipalityInFinland": true,
                "guardianInfo": "",
                "supportContactsInfo": "",
                "partnerOrganisations": []
              },
              "studentCase": {
                "openedAt": "2023-12-07",
                "assignedTo": null,
                "source": "VALPAS_AUTOMATIC_CHECK",
                "sourceValpas": null,
                "sourceOther": null,
                "sourceContact": "",
                "schoolBackground": [],
                "caseBackgroundReasons": [],
                "notInSchoolReason": null
              }
            }
            """.trimIndent()

        http.post("/students", studentBody, token)
        val result = http.post("/students", studentBody, token)

        assertEquals(409, result.statusCode)
        assertTrue(result.body.contains("errorCode"), "Response must contain 'errorCode' field: ${result.body}")
        assertFalse(result.body.contains("\"errorCode\":null"), "errorCode must not be null for UniqueConstraintViolation: ${result.body}")
    }

    // -------------------------------------------------------------------------
    // Unauthorized -> 401 (via HttpAccessControl filter, not ExceptionHandler,
    // but the 401 status is the contract the frontend depends on)
    // -------------------------------------------------------------------------

    @Test
    fun `missing token returns 401`() {
        val result = http.get("/employees")
        assertEquals(401, result.statusCode)
    }

    @Test
    fun `invalid token returns 401`() {
        val result = http.get("/employees", "not-a-valid-jwt")
        assertEquals(401, result.statusCode)
    }

    // -------------------------------------------------------------------------
    // Forbidden -> 403 (via HttpAccessControl filter)
    // -------------------------------------------------------------------------

    @Test
    fun `non-system user on system endpoint returns 403`() {
        val result = http.get("/system/users/${UUID.randomUUID()}", token)
        assertEquals(403, result.statusCode)
    }
}
