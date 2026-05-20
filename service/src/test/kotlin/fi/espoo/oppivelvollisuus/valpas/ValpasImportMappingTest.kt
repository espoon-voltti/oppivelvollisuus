// SPDX-FileCopyrightText: 2025-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValpasImportMappingTest {
    @Test
    fun `buildAddressString returns empty when contacts are null`() {
        assertEquals("", buildAddressString(null))
    }

    @Test
    fun `buildAddressString concatenates non-blank fields with comma, never country`() {
        val y =
            ValpasYhteystiedot(
                puhelinnumero = null,
                email = null,
                lähiosoite = "Mannerheimintie 1",
                postinumero = "00100",
                postitoimipaikka = "Helsinki",
            )
        assertEquals("Mannerheimintie 1, 00100, Helsinki", buildAddressString(y))
    }

    @Test
    fun `buildAddressString skips blank fields`() {
        val y =
            ValpasYhteystiedot(
                puhelinnumero = null,
                email = null,
                lähiosoite = "",
                postinumero = "00100",
                postitoimipaikka = "Helsinki",
            )
        assertEquals("00100, Helsinki", buildAddressString(y))
    }

    @Test
    fun `mapToStudentInput fills expected fields with valpas link`() {
        val oppija =
            ValpasOppija(
                oppijanumero = "1.2.246.562.24.00000000123",
                etunimet = "Liisa Maria",
                sukunimi = "Virtanen",
                syntymäaika = LocalDate.of(2010, 6, 15),
                hetu = "150610A123B",
                aktiivinenKuntailmoitus =
                    ValpasKuntailmoitus(
                        id = UUID.randomUUID(),
                        aikaleima = LocalDate.of(2026, 5, 1),
                        oppijanYhteystiedot =
                            ValpasYhteystiedot(
                                puhelinnumero = "+358501234567",
                                email = "liisa@example.fi",
                                lähiosoite = "Mannerheimintie 1",
                                postinumero = "00100",
                                postitoimipaikka = "Helsinki",
                            ),
                    ),
            )
        val input =
            mapToStudentInput(oppija, opintopolkuBaseUrl = "https://virkailija.opintopolku.fi")
        assertEquals(
            "https://virkailija.opintopolku.fi/valpas/virkailija/oppija/1.2.246.562.24.00000000123",
            input.valpasLink,
        )
        assertEquals("1.2.246.562.24.00000000123", input.valpasOppijaOid)
        assertEquals("150610A123B", input.ssn)
        assertEquals("Liisa Maria", input.firstName)
        assertEquals("Virtanen", input.lastName)
        assertEquals(LocalDate.of(2010, 6, 15), input.dateOfBirth)
        assertEquals("+358501234567", input.phone)
        assertEquals("liisa@example.fi", input.email)
        assertEquals("Mannerheimintie 1, 00100, Helsinki", input.address)
        assertEquals(true, input.municipalityInFinland)
    }

    @Test
    fun `mapToStudentInput leaves valpas link blank when baseUrl is null`() {
        val oppija =
            ValpasOppija(
                oppijanumero = "x",
                etunimet = "A",
                sukunimi = "B",
                syntymäaika = LocalDate.of(2010, 1, 1),
                hetu = null,
                aktiivinenKuntailmoitus = null,
            )
        val input = mapToStudentInput(oppija, opintopolkuBaseUrl = null)
        assertEquals("", input.valpasLink)
        assertEquals("", input.ssn)
        assertEquals("", input.phone)
        assertEquals("", input.address)
    }
}
