import React from 'react'
import styled from 'styled-components'

import { colors } from '../shared/theme'

import { CaseStatus, caseStatusNames } from './enums'

const chipColors: Record<CaseStatus, string> = {
  TODO: colors.main.m1,
  ON_HOLD: colors.status.success,
  FINISHED: colors.grayscale.g15
}

const Chip = styled.div<{ $status: CaseStatus }>`
  color: ${(p) =>
    p.$status === 'FINISHED' ? colors.grayscale.g70 : colors.grayscale.g0};
  background-color: ${(p) => chipColors[p.$status]};
  font-weight: 600;
  border-radius: 16px;
  padding: 4px 16px;
  flex-grow: 0;
  width: fit-content;
`

export const StatusChip = React.memo(function StatusChip({
  status
}: {
  status: CaseStatus
}) {
  return <Chip $status={status}>{caseStatusNames[status]}</Chip>
})
