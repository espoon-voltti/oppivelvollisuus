// SPDX-FileCopyrightText: 2025-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import fi.espoo.oppivelvollisuus.shared.config.defaultJsonMapperBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

/**
 * Parses the real sample response captured from the Valpas test environment at
 * `service/src/test/resources/valpas/sample-result.json`. Asserts shape and presence (not specific
 * values) so the fixture can drift over time without breaking the test.
 *
 * To regenerate the fixture: POST a query of type `eiSuoritaOppivelvollisuutta` to
 * https://virkailija.testiopintopolku.fi/koski/valpas/api/massaluovutus with test-env credentials,
 * poll until complete, download the resulting JSON file, and pretty-print it.
 */
class ValpasResultFileParsingTest {
    private val jsonMapper: JsonMapper =
        defaultJsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    @Test
    fun `parses the sample Valpas result file`() {
        val stream =
            checkNotNull(javaClass.classLoader.getResourceAsStream("valpas/sample-result.json")) {
                "sample-result.json not found on test classpath"
            }
        val result = stream.use { jsonMapper.readValue(it, ValpasResultFile::class.java) }

        assertTrue(result.oppijat.isNotEmpty(), "expected at least one oppija in sample")
        val oppija = result.oppijat.first()
        assertTrue(oppija.oppijanumero.isNotBlank())
        assertTrue(oppija.etunimet.isNotBlank())
        assertTrue(oppija.sukunimi.isNotBlank())
        assertNotNull(oppija.syntymäaika, "syntymäaika should deserialize as LocalDate")
        assertNotNull(oppija.hetu, "test fixture should include hetu")

        val notification =
            checkNotNull(oppija.aktiivinenKuntailmoitus) {
                "test fixture should include aktiivinenKuntailmoitus"
            }
        assertNotNull(notification.id, "notification UUID should parse")
        assertNotNull(notification.aikaleima, "aikaleima should parse as LocalDate")

        val contacts =
            checkNotNull(notification.oppijanYhteystiedot) {
                "test fixture should include oppijanYhteystiedot"
            }
        // Finnish-character field name must round-trip via Jackson
        assertNotNull(contacts.lähiosoite, "lähiosoite (Finnish ä) should deserialize")
    }
}
