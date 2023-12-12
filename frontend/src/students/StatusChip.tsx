import React from 'react'

import { StaticChip } from '../shared/Chip'
import { colors } from '../shared/theme'

import { CaseStatus, caseStatusNames } from './enums'

const chipColors: Record<CaseStatus, string> = {
  TODO: colors.main.m1,
  ON_HOLD: '#148190',
  FINISHED: colors.grayscale.g15
}

export const StatusChip = React.memo(function StatusChip({
  status
}: {
  status: CaseStatus
}) {
  return (
    <StaticChip
      $color={chipColors[status]}
      $textColor={
        status === 'FINISHED' ? colors.grayscale.g70 : colors.grayscale.g0
      }
      style={{ width: '180px', textAlign: 'center' }}
    >
      {caseStatusNames[status]}
    </StaticChip>
  )
})
