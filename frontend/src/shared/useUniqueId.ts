import { useMemo } from 'react'

let nextId = 1

export function useUniqueId(): string {
  return useMemo(() => `generated-id-${nextId++}`, [])
}
