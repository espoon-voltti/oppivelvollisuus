// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const genders = ['FEMALE', 'MALE', 'OTHER'] as const

export type Gender = (typeof genders)[number]

export const genderNames: Record<Gender, string> = {
  FEMALE: 'Tyttö',
  MALE: 'Poika',
  OTHER: 'Muu'
}
