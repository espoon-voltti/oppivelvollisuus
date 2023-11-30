import { faCheckCircle } from '@fortawesome/free-regular-svg-icons/faCheckCircle'
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons/faExclamationTriangle'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { BaseProps, colors } from '../theme'

export const StatusIcon = styled(FontAwesomeIcon)`
  font-size: 15px;
  margin-left: 8px;
`

export type InfoStatus = 'warning' | 'success'

interface UnderRowStatusIconProps extends BaseProps {
  status?: InfoStatus
}

export const UnderRowStatusIcon = React.memo(function UnderRowStatusIcon({
  status
}: UnderRowStatusIconProps) {
  return status === 'warning' ? (
    <StatusIcon icon={faExclamationTriangle} color={colors.status.warning} />
  ) : status === 'success' ? (
    <StatusIcon icon={faCheckCircle} color={colors.status.success} />
  ) : null
})
