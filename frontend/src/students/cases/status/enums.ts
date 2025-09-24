// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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

export const followUpMeasureValues = [
  'KELA_REHABILITATION_SERVICES',
  'SOCIAL_SERVICES',
  'YOUTH_WORK',
  'JOB_SEARCH_SUPPORT',
  'LANGUAGE_COURSE',
  'MISSING',
  'MOVE_ABROAD'
] as const

export type FollowUpMeasure = (typeof followUpMeasureValues)[number]

export const followUpMeasureNames: Record<FollowUpMeasure, string> = {
  KELA_REHABILITATION_SERVICES: 'Kelan kuntouttavat palvelut',
  SOCIAL_SERVICES: 'Sosiaalitoimen palvelut',
  YOUTH_WORK: 'Etsivä nuorisotyö',
  JOB_SEARCH_SUPPORT: 'Työnhaun tukeminen',
  LANGUAGE_COURSE: 'Kielikurssi',
  MISSING: 'Kadonnut',
  MOVE_ABROAD: 'Muutto ulkomaille'
}

export const schoolTypes = [
  'PERUSOPETUKSEEN_VALMISTAVA',
  'AIKUISTEN_PERUSOPETUS',
  'AIKUISTEN_PERUSOPETUS_SUOMEEN_MUUTTANEILLE',
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
  AIKUISTEN_PERUSOPETUS_SUOMEEN_MUUTTANEILLE:
    'Aikuisten perusopetus Suomeen muuttaneille',
  AMMATILLINEN_PERUSTUTKINTO: 'Ammatillinen perustutkinto',
  LUKIO: 'Lukio ',
  AIKUISLUKIO: 'Aikuislukio',
  YLEISOPPILAITOKSEN_TUVA: 'Yleisoppilaitoksen tuva',
  AMMATILLISEN_OPPILAITOKSEN_TUVA: 'Ammatillisen erityisoppilaitoksen tuva',
  AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO:
    'Ammatillisen erityisoppilaitoksen perustutkinto',
  TELMA: 'Telma',
  KANSANOPISTO: 'Kansanopisto',
  OTHER: 'Muu'
}
