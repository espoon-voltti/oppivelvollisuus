// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const caseSources = [
  'VALPAS_NOTICE',
  'VALPAS_AUTOMATIC_CHECK',
  'OTHER'
] as const

export type CaseSource = (typeof caseSources)[number]

export const caseSourceNames: Record<CaseSource, string> = {
  VALPAS_NOTICE: 'Valpas - ilmoitus',
  VALPAS_AUTOMATIC_CHECK: 'Valpas - automaattinen tarkistus',
  OTHER: 'Muu ilmoitus asuinkuntaan'
}

export const caseSourceNamesShort: Record<CaseSource, string> = {
  VALPAS_NOTICE: 'Valpas',
  VALPAS_AUTOMATIC_CHECK: 'Automaattinen tarkistus',
  OTHER: 'Muu ilmoitus'
}

export const valpasNotifiers = [
  'PERUSOPETUS',
  'AIKUISTEN_PERUSOPETUS',
  'AMMATILLINEN_PERUSTUTKINTO',
  'LUKIO',
  'AIKUISLUKIO',
  'YLEISOPPILAITOKSEN_TUVA',
  'AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO',
  'AMMATILLISEN_ERITYISOPPILAITOKSEN_TUVA',
  'TELMA',
  'TOINEN_ASUINKUNTA',
  'OPISTO'
] as const

export type ValpasNotifier = (typeof valpasNotifiers)[number]

export const valpasNotifierNames: Record<ValpasNotifier, string> = {
  PERUSOPETUS: 'Perusopetus',
  AIKUISTEN_PERUSOPETUS: 'Aikuisten perusopetus',
  AMMATILLINEN_PERUSTUTKINTO: 'Ammatillinen perustutkinto',
  LUKIO: 'Lukio ',
  AIKUISLUKIO: 'Aikuislukio',
  YLEISOPPILAITOKSEN_TUVA: 'Yleisoppilaitoksen tuva',
  AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO:
    'Ammatillisen erityisoppilaitoksen perustutkinto',
  AMMATILLISEN_ERITYISOPPILAITOKSEN_TUVA:
    'Ammatillisen erityisoppilaitoksen tuva',
  TELMA: 'Telma',
  TOINEN_ASUINKUNTA: 'Toinen asuinkunta',
  OPISTO: 'Opisto'
}

export const otherNotifiers = [
  'ENNAKOIVA_OHJAUS',
  'TYOLLISYYSPALVELUT',
  'OMA_YHTEYDENOTTO',
  'OHJAAMOTALO',
  'OPPILAITOS',
  'LASTENSUOJELU',
  'OTHER'
] as const

export type OtherNotifier = (typeof otherNotifiers)[number]

export const otherNotifierNames: Record<OtherNotifier, string> = {
  ENNAKOIVA_OHJAUS: 'Ennakoiva ohjaus',
  TYOLLISYYSPALVELUT: 'Työllisyyspalvelut',
  OMA_YHTEYDENOTTO: 'Oma yhteydenotto huoltaja / nuori ',
  OHJAAMOTALO: 'Ohjaamotalo',
  OPPILAITOS: 'Oppilaitos',
  LASTENSUOJELU: 'Lastensuojelu',
  OTHER: 'Muu taho'
}

export const schoolBackgrounds = [
  'PERUSKOULUN_PAATTOTODISTUS',
  'PERUSOPETUKSEEN_VALMISTAVA_OPISKELU_SUOMESSA',
  'ULKOMAILLA_SUORITETUT_PERUSOPETUSTA_VASTAAVAT_OPINNOT',
  'EI_PERUSKOULUN_PAATTOTODISTUSTA',
  'KESKEYTYNEET_TOISEN_ASTEEN_OPINNOT',
  'KESKEYTYNEET_NIVELVAIHEEN_OPINNOT',
  'VSOP_PERUSKOULUSSA',
  'YLEINEN_TUKI_PERUSKOULUSSA',
  'TEHOSTETTU_TUKI_PERUSKOULUSSA',
  'TEHOSTETTU_HENKKOHT_TUKI_PERUSKOULUSSA',
  'ERITYISEN_TUEN_PAATOS_PERUSKOULUSSA',
  'YKSILOLLISTETTY_OPPIMAARA_AIDINKIELESSA_JA_MATEMATIIKASSA'
]

export type SchoolBackground = (typeof schoolBackgrounds)[number]

export const schoolBackgroundNames: Record<SchoolBackground, string> = {
  PERUSKOULUN_PAATTOTODISTUS: 'Peruskoulun päättötodistus',
  EI_PERUSKOULUN_PAATTOTODISTUSTA: 'Ei peruskoulun päättötodistusta',
  KESKEYTYNEET_TOISEN_ASTEEN_OPINNOT: 'Keskeytyneet 2. asteen opinnot',
  KESKEYTYNEET_NIVELVAIHEEN_OPINNOT: 'Keskeytyneet nivelvaiheen opinnot',
  VSOP_PERUSKOULUSSA: 'VSOP (vuosiluokkiin sitomaton opiskelu) peruskoulussa',
  YLEINEN_TUKI_PERUSKOULUSSA: 'Yleinen tuki peruskoulussa',
  TEHOSTETTU_TUKI_PERUSKOULUSSA: 'Tehostettu tuki peruskoulussa',
  TEHOSTETTU_HENKKOHT_TUKI_PERUSKOULUSSA:
    'Tehostettu henkilökohtainen tuki peruskoulussa',
  ERITYISEN_TUEN_PAATOS_PERUSKOULUSSA: 'Erityisen tuen päätös peruskoulussa',
  YKSILOLLISTETTY_OPPIMAARA_AIDINKIELESSA_JA_MATEMATIIKASSA:
    'Yksilöllistetty oppimäärä sekä äidinkielessä että matematiikassa',
  PERUSOPETUKSEEN_VALMISTAVA_OPISKELU_SUOMESSA:
    'Perusopetukseen valmistava opiskelu Suomessa',
  ULKOMAILLA_SUORITETUT_PERUSOPETUSTA_VASTAAVAT_OPINNOT:
    'Ulkomailla suoritetut perusopetusta vastaavat opinnot'
}

export const caseBackgroundReasonValues = [
  'MOTIVAATION_PUUTE',
  'VAARA_ALAVALINTA',
  'OPPIMISVAIKEUDET',
  'ELAMANHALLINNAN_HAASTEET',
  'POISSAOLOT',
  'TERVEYDELLISET_PERUSTEET',
  'MUUTTO_PAIKKAKUNNALLE',
  'MUUTTO_ULKOMAILLE',
  'MAAHAN_MUUTTANUT_NUORI_ILMAN_OPISKELUPAIKKAA',
  'JAANYT_ILMAN_OPISKELUPAIKKAA',
  'MUU_SYY'
]

export type CaseBackgroundReason = (typeof caseBackgroundReasonValues)[number]

export const caseBackgroundReasonNames: Record<CaseBackgroundReason, string> = {
  MOTIVAATION_PUUTE: 'Motivaation puute',
  VAARA_ALAVALINTA: 'Väärä alavalinta',
  OPPIMISVAIKEUDET: 'Oppimisvaikeudet',
  ELAMANHALLINNAN_HAASTEET: 'Elämänhallinnan haasteet',
  POISSAOLOT: 'Poissaolot',
  TERVEYDELLISET_PERUSTEET: 'Terveydelliset perusteet',
  MUUTTO_PAIKKAKUNNALLE: 'Muutto paikkakunnalle',
  MUUTTO_ULKOMAILLE: 'Muutto ulkomaille',
  MAAHAN_MUUTTANUT_NUORI_ILMAN_OPISKELUPAIKKAA:
    'Maahan muuttanut nuori ilman opiskelupaikkaa',
  JAANYT_ILMAN_OPISKELUPAIKKAA:
    'Peruskoulun päättänyt jäänyt ilman opiskelupaikkaa',
  MUU_SYY: 'Muu syy'
}

export const notInSchoolReasons = [
  'KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA',
  'EI_OLE_VASTAANOTTANUT_SAAMAANSA_OPISKELUPAIKKAA',
  'EI_OLE_ALOITTANUT_VASTAANOTTAMASSAAN_OPISKELUPAIKASSA',
  'EI_OLE_HAKEUTUNUT_JATKO_OPINTOIHIN',
  'EI_OPISKELUPAIKKAA_YLEISOPPILAITOKSESSA',
  'EI_OPISKELUPAIKKAA_AMMATILLISESSA_ERITYISOPPILAITOKSESSA',
  'EI_OLE_SAANUT_OPISKELUPAIKKAA_KIELITAIDON_VUOKSI',
  'OPINNOT_ULKOMAILLA',
  'MUU_SYY'
]

export type NotInSchoolReason = (typeof notInSchoolReasons)[number]

export const notInSchoolReasonNames: Record<NotInSchoolReason, string> = {
  KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA: 'Katsottu eronneeksi oppilaitoksesta',
  EI_OLE_VASTAANOTTANUT_SAAMAANSA_OPISKELUPAIKKAA:
    'Ei ole vastaanottanut saamaansa opiskelupaikkaa',
  EI_OLE_ALOITTANUT_VASTAANOTTAMASSAAN_OPISKELUPAIKASSA:
    'Ei ole aloittanut vastaanottamassaan opiskelupaikassa',
  EI_OLE_HAKEUTUNUT_JATKO_OPINTOIHIN: 'Ei ole hakeutunut jatko-opintoihin',
  EI_OPISKELUPAIKKAA_YLEISOPPILAITOKSESSA:
    'Jäänyt ilman opiskelupaikkaa yleisoppilaitoksessa',
  EI_OPISKELUPAIKKAA_AMMATILLISESSA_ERITYISOPPILAITOKSESSA:
    'Jäänyt ilman opiskelupaikkaa ammatillisessa erityisoppilaitoksessa',
  EI_OLE_SAANUT_OPISKELUPAIKKAA_KIELITAIDON_VUOKSI:
    'Ei ole saanut opiskelupaikkaa 0-kielitaidon vuoksi',
  OPINNOT_ULKOMAILLA: 'Opinnot ulkomailla (muu kuin vaihto-opiskelu)',
  MUU_SYY: 'Muu syy'
}

export const partnerOrganisationValues = [
  'LASTENSUOJELU',
  'TERVEYDENHUOLTO',
  'MIELENTERVEYSPALVELUT',
  'TUKIHENKILO',
  'TYOPAJATOIMINTA'
]

export type PartnerOrganisation = (typeof partnerOrganisationValues)[number]

export const partnerOrganisationNames: Record<PartnerOrganisation, string> = {
  LASTENSUOJELU: 'Lastensuojelu',
  TERVEYDENHUOLTO: 'Terveydenhuolto',
  MIELENTERVEYSPALVELUT: 'Mielenterveys- ja päihdepalvelut',
  TUKIHENKILO: 'Tukihenkilö',
  TYOPAJATOIMINTA: 'Työpajatoiminta'
}
