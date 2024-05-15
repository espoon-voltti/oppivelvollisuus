// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'

export function useWarnOnUnsavedChanges(dirty: boolean) {
  useEffect(() => {
    // Support different browsers: https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event
    const warningTextForOldBrowsers = 'Haluatko varmasti poistua tallentamatta?'

    const beforeUnloadHandler = (e: BeforeUnloadEvent) => {
      if (dirty) {
        e.preventDefault()
        e.returnValue = warningTextForOldBrowsers
        return warningTextForOldBrowsers
      }
      return
    }

    window.addEventListener('beforeunload', beforeUnloadHandler)
    return () => {
      window.removeEventListener('beforeunload', beforeUnloadHandler)
    }
  }, [dirty])
}
