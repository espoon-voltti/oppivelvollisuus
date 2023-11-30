import { faPlus } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React from 'react'
import styled from 'styled-components'

import { BaseProps, colors } from '../theme'

import { defaultButtonTextStyle } from './Button'

const StyledButton = styled.button`
  width: fit-content;

  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  padding: 0;

  background: none;
  border: none;
  outline: none;
  cursor: pointer;

  &.disabled {
    color: ${colors.grayscale.g70};
    cursor: not-allowed;

    .icon-wrapper-inner {
      background: ${colors.grayscale.g70};
    }
  }

  .icon-wrapper-outer {
    height: 43px !important;
    width: 43px !important;
    display: flex;
    justify-content: center;
    align-items: center;
    box-sizing: border-box;
    margin: -4px 8px -4px -4px;
  }

  &:focus .icon-wrapper-outer {
    border: 2px solid ${colors.main.m2Focus};
    border-radius: 100%;
  }

  .icon-wrapper-inner {
    height: 35px !important;
    width: 35px !important;
    display: flex;
    justify-content: center;
    align-items: center;

    font-size: 18px;
    color: ${colors.grayscale.g0};
    font-weight: 400;
    background: ${colors.main.m2};
    border-radius: 100%;
  }

  &.icon-right {
    .icon-wrapper-outer {
      margin: -4px -4px -4px 16px;
    }
  }

  ${defaultButtonTextStyle};
`

export interface AddButtonProps extends BaseProps {
  text: string
  onClick: () => unknown
  disabled?: boolean
  iconRight?: boolean
}

const AddButton = React.memo(function AddButton({
  className,
  'data-qa': dataQa,
  text,
  onClick,
  disabled = false,
  iconRight = false
}: AddButtonProps) {
  return (
    <StyledButton
      type="button"
      className={classNames(className, { disabled, iconRight })}
      data-qa={dataQa}
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
    >
      {iconRight && <span>{text}</span>}
      <div className="icon-wrapper-outer">
        <div className="icon-wrapper-inner">
          <FontAwesomeIcon icon={faPlus} />
        </div>
      </div>
      {!iconRight && <span>{text}</span>}
    </StyledButton>
  )
})

export default AddButton
