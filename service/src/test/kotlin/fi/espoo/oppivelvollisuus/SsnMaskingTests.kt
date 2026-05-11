// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.config.SsnMasker
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks in SSN masking behavior of [SsnMasker].
 *
 * The masker is wired as a [net.logstash.logback.mask.ValueMasker] on the
 * VOLTTI_DEFAULT_APPENDER_SANITIZED encoder (default-appender-sanitized.xml).
 * It replaces Finnish SSN patterns with "REDACTED-SSN".
 *
 * Phase 2 replaces SsnMasker with shared/config/SsnMasker; these tests must stay green.
 *
 * Note on scope: the masker is only active in the `!local` Spring profile (i.e., production).
 * Tests run without a Spring profile, so no appender has the masker attached. We directly
 * unit-test the masker class to lock in its regex logic independently of the appender wiring.
 */
class SsnMaskingTests {
    private val masker = SsnMasker()

    private fun mask(value: String): Any = masker.mask(null, value)

    // -------------------------------------------------------------------------
    // Mask pattern: REDACTED-SSN
    // -------------------------------------------------------------------------

    @Test
    fun `mask replacement string is REDACTED-SSN`() {
        val result = mask("170108A927R")
        assertEquals("REDACTED-SSN", result)
    }

    // -------------------------------------------------------------------------
    // Valid SSN formats that must be masked
    // Finnish SSN format: DDMMYY[separator][3 digits][checkchar]
    // Separators: - + A B C D E F U V W X Y (century indicators)
    // -------------------------------------------------------------------------

    @Test
    fun `SSN with hyphen separator is masked`() {
        val result = mask("170108-9270")
        assertEquals("REDACTED-SSN", result)
    }

    @Test
    fun `SSN with plus separator is masked`() {
        // + indicates born in 1800s
        val result = mask("170108+9270")
        assertEquals("REDACTED-SSN", result)
    }

    @Test
    fun `SSN with A separator (born 2000s) is masked`() {
        val result = mask("170108A927R")
        assertEquals("REDACTED-SSN", result)
    }

    @Test
    fun `SSN with B separator is masked`() {
        val result = mask("170108B9270")
        assertEquals("REDACTED-SSN", result)
    }

    @Test
    fun `SSN with uppercase check character is masked`() {
        val result = mask("170108-927F")
        assertEquals("REDACTED-SSN", result)
    }

    @Test
    fun `SSN embedded in longer text is masked`() {
        val result = mask("Student ssn: 170108A927R enrolled")
        val resultStr = result.toString()
        assertFalse(resultStr.contains("170108A927R"), "SSN must be removed from text: $resultStr")
        assertTrue(resultStr.contains("REDACTED-SSN"), "SSN must be replaced with REDACTED-SSN: $resultStr")
    }

    @Test
    fun `multiple SSNs in same string are all masked`() {
        val result = mask("First: 170108A927R, Second: 100507A967F")
        val resultStr = result.toString()
        assertFalse(resultStr.contains("170108A927R"), "First SSN must be masked: $resultStr")
        assertFalse(resultStr.contains("100507A967F"), "Second SSN must be masked: $resultStr")
        // Two replacements
        assertEquals(2, resultStr.split("REDACTED-SSN").size - 1, "Both SSNs must be replaced: $resultStr")
    }

    // -------------------------------------------------------------------------
    // Non-SSN strings must NOT be masked
    // -------------------------------------------------------------------------

    @Test
    fun `plain text without SSN is unchanged`() {
        val input = "Hello, world!"
        val result = mask(input)
        assertEquals(input, result)
    }

    @Test
    fun `UUID is not masked`() {
        val input = "a3f1c2d4-e5b6-7890-abcd-ef1234567890"
        val result = mask(input)
        assertEquals(input, result)
    }

    @Test
    fun `date alone is not masked`() {
        val input = "2023-12-07"
        val result = mask(input)
        assertEquals(input, result)
    }

    @Test
    fun `6-digit sequence without separator is not masked`() {
        val input = "170108"
        val result = mask(input)
        assertEquals(input, result)
    }

    // -------------------------------------------------------------------------
    // Non-string values pass through unchanged (except null -> "null")
    // -------------------------------------------------------------------------

    @Test
    fun `integer value is passed through unchanged`() {
        val result = masker.mask(null, 42)
        assertEquals(42, result)
    }

    @Test
    fun `null value becomes string null`() {
        val result = masker.mask(null, null)
        assertEquals("null", result)
    }

    @Test
    fun `boolean value is passed through unchanged`() {
        val result = masker.mask(null, true)
        assertEquals(true, result)
    }

    // -------------------------------------------------------------------------
    // Edge: SSN must not be masked if preceded by a digit or letter (negative lookbehind)
    // -------------------------------------------------------------------------

    @Test
    fun `SSN preceded by digit is not masked`() {
        // Regex has negative lookbehind for digits — but this is a subtle edge case.
        // The lookbehind is (?<!-|[\dA-z]) so a digit before the SSN prevents matching.
        val input = "1170108A927R"
        val result = mask(input)
        // The full 12-char sequence starts with a digit so the lookbehind fires
        assertEquals(input, result.toString())
    }
}
