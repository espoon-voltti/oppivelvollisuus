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
  'TOINEN_ASUINKUNTA'
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
  TOINEN_ASUINKUNTA: 'Toinen asuinkunta'
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
