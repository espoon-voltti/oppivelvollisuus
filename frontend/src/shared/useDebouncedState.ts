import React, { useEffect, useState } from 'react'

export const useDebouncedState = <T>(
  initialState: T,
  delay = 500
): [T, React.Dispatch<React.SetStateAction<T>>, T] => {
  const [debouncedState, setDebouncedState] = useState(initialState)
  const [latestState, setLatestState] = useState(initialState)

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedState(latestState)
    }, delay)

    return () => {
      clearTimeout(handler)
    }
  }, [latestState, delay])

  return [latestState, setLatestState, debouncedState]
}
