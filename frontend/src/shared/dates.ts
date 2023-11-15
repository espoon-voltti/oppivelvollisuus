import { format, parse } from 'date-fns'

export const parseDate = (date: string) => {
  try {
    return parse(date, 'dd.MM.yyyy', new Date())
  } catch (e) {
    return undefined
  }
}
export const formatDate = (date: Date) => format(date, 'dd.MM.yyyy')
