import React, { useEffect, useState } from 'react'

import { apiGetHellos, HelloResponse } from './api'

export const HelloWorldPage = React.memo(function HelloWorldPage() {
  const [helloResponse, setHelloResponse] = useState<HelloResponse | null>(null)
  useEffect(() => {
    void apiGetHellos().then(setHelloResponse)
  }, [])

  return (
    <div>
      <h1>Oppivelvollisuus</h1>
      {helloResponse && <div>Rivejä kannassa: {helloResponse.rows}</div>}
    </div>
  )
})
