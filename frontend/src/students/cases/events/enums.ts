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
