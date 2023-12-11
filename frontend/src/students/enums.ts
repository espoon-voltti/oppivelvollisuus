export const caseEventTypes = [
  'NOTE',
  'EXPLANATION_REQUEST',
  'EDUCATION_SUSPENSION_APPLICATION_RECEIVED',
  'EDUCATION_SUSPENSION_GRANTED',
  'EDUCATION_SUSPENSION_DENIED',
  'CHILD_PROTECTION_NOTICE',
  'HEARING_LETTER',
  'HEARING',
  'DIRECTED_TO_YLEISOPPILAITOKSEN_TUVA',
  'DIRECTED_TO_ERITYISOPPILAITOKSEN_TUVA',
  'DIRECTED_TO_ERITYISOPPILAITOKSEN_TELMA'
] as const

export type CaseEventType = (typeof caseEventTypes)[number]

export const caseEventTypeNames: Record<CaseEventType, string> = {
  NOTE: 'Muistiinpano',
  EXPLANATION_REQUEST: 'Selvityspyyntö',
  EDUCATION_SUSPENSION_APPLICATION_RECEIVED:
    'Keskeytyshakemus saapunut asuinkuntaan',
  EDUCATION_SUSPENSION_GRANTED: 'Määräaikainen keskeytys myönnetty',
  EDUCATION_SUSPENSION_DENIED: 'Keskeytyshakemus hylätty',
  CHILD_PROTECTION_NOTICE: 'Lastensuojeluilmoitus',
  HEARING_LETTER: 'Kuulemiskirje',
  HEARING: 'Kuuleminen',
  DIRECTED_TO_YLEISOPPILAITOKSEN_TUVA: 'Osoitus - Yleisoppilaitoksen tuva',
  DIRECTED_TO_ERITYISOPPILAITOKSEN_TUVA: 'Osoitus - Erityisoppilaitoksen tuva',
  DIRECTED_TO_ERITYISOPPILAITOKSEN_TELMA: 'Osoitus - Erityisoppilaitoksen telma'
}

export const caseStatuses = ['TODO', 'ON_HOLD', 'FINISHED'] as const

export type CaseStatus = (typeof caseStatuses)[number]

export const caseStatusNames: Record<CaseStatus, string> = {
  TODO: 'Selvittämättä',
  ON_HOLD: 'Edistynyt - odottaa',
  FINISHED: 'Ohjaus päättynyt'
}

export const caseFinishedReasons = [
  'BEGAN_STUDIES',
  'COMPULSORY_EDUCATION_ENDED',
  'COMPULSORY_EDUCATION_SUSPENDED',
  'COMPULSORY_EDUCATION_SUSPENDED_PERMANENTLY',
  'MOVED_TO_ANOTHER_MUNICIPALITY',
  'MOVED_ABROAD',
  'ERRONEOUS_NOTICE',
  'OTHER'
] as const

export type CaseFinishedReason = (typeof caseFinishedReasons)[number]

export const caseFinishedReasonNames: Record<CaseFinishedReason, string> = {
  BEGAN_STUDIES: 'Siirtynyt opiskelemaan',
  COMPULSORY_EDUCATION_ENDED: 'Oppivelvollisuus päättynyt',
  COMPULSORY_EDUCATION_SUSPENDED: 'Oppivelvollisuus keskeytetty toistaiseksi',
  COMPULSORY_EDUCATION_SUSPENDED_PERMANENTLY:
    'Oppivelvollisuus keskeytetty pysyvästi',
  MOVED_TO_ANOTHER_MUNICIPALITY: 'Muutto toiselle paikkakunnalle',
  MOVED_ABROAD: 'Muutto ulkomaille',
  ERRONEOUS_NOTICE: 'Virheellinen ilmoitus',
  OTHER: 'Muu'
}

export const schoolTypes = [
  'PERUSOPETUKSEEN_VALMISTAVA',
  'AIKUISTEN_PERUSOPETUS',
  'AMMATILLINEN_PERUSTUTKINTO',
  'LUKIO',
  'AIKUISLUKIO',
  'YLEISOPPILAITOKSEN_TUVA',
  'AMMATILLISEN_OPPILAITOKSEN_TUVA',
  'AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO',
  'TELMA',
  'KANSANOPISTO',
  'OTHER'
] as const

export type SchoolType = (typeof schoolTypes)[number]

export const schoolTypeNames: Record<SchoolType, string> = {
  PERUSOPETUKSEEN_VALMISTAVA: 'Perusopetukseen valmistava',
  AIKUISTEN_PERUSOPETUS: 'Aikuisten perusopetus',
  AMMATILLINEN_PERUSTUTKINTO: 'Ammatillinen perustutkinto',
  LUKIO: 'Lukio ',
  AIKUISLUKIO: 'Aikuislukio',
  YLEISOPPILAITOKSEN_TUVA: 'Yleisoppilaitoksen tuva',
  AMMATILLISEN_OPPILAITOKSEN_TUVA: 'Ammatillisen oppilaitoksen tuva',
  AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO:
    'Ammatillisen erityisoppilaitoksen perustutkinto',
  TELMA: 'Telma',
  KANSANOPISTO: 'Kansanopisto',
  OTHER: 'Muu'
}
