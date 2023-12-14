export const genders = ['FEMALE', 'MALE', 'OTHER'] as const

export type Gender = (typeof genders)[number]

export const genderNames: Record<Gender, string> = {
  FEMALE: 'Tyttö',
  MALE: 'Poika',
  OTHER: 'Muu'
}
