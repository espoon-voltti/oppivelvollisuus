import { faChevronDown } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, {
  ChangeEvent,
  FocusEventHandler,
  useCallback,
  useMemo
} from 'react'
import styled from 'styled-components'

import { colors, InputWidth, inputWidthCss } from '../theme'

export interface SelectProps<T> {
  id?: string
  items: readonly T[]
  selectedItem: T | null
  onChange: (item: T | null) => void
  disabled?: boolean
  placeholder?: string
  width?: InputWidth
  getItemLabel?: (item: T) => string
  getItemValue?: (item: T) => string
  getItemDataQa?: (item: T) => string | undefined
  name?: string
  onFocus?: FocusEventHandler<HTMLSelectElement>
  'data-qa'?: string
}

function GenericSelect<T>(props: SelectProps<T>) {
  const {
    id,
    name,
    'data-qa': dataQa,
    items,
    selectedItem,
    getItemLabel = (item) => String(item),
    getItemValue = (item) => String(item),
    getItemDataQa,
    onChange,
    onFocus,
    width,
    placeholder,
    disabled
  } = props

  const options = useMemo(
    () =>
      items.map((item) => ({
        value: getItemValue(item),
        label: getItemLabel(item),
        dataQa: getItemDataQa?.(item)
      })),
    [getItemDataQa, getItemLabel, getItemValue, items]
  )

  const handleChange = useCallback(
    (e: ChangeEvent<HTMLSelectElement>) => {
      const newSelectedItem =
        items.find((item) => getItemValue(item) === e.target.value) ?? null
      onChange(newSelectedItem)
    },
    [items, getItemValue, onChange]
  )

  return (
    <Root $width={width} data-qa={dataQa}>
      <Wrapper>
        <StyledSelect
          id={id}
          name={name}
          value={selectedItem ? getItemValue(selectedItem) : ''}
          onChange={handleChange}
          disabled={disabled}
          onFocus={onFocus}
        >
          {placeholder !== undefined && <option value="">{placeholder}</option>}
          {options.map((item) => (
            <option key={item.value} value={item.value} data-qa={item.dataQa}>
              {item.label}
            </option>
          ))}
        </StyledSelect>
        <Icon size="sm" icon={faChevronDown} />
      </Wrapper>
    </Root>
  )
}

export const Root = styled.div<{ $width: InputWidth | undefined }>`
  ${(p) => (p.$width ? inputWidthCss(p.$width) : '')}
  border-radius: 2px;
  border: 2px solid transparent;

  &.active {
    border-color: ${colors.main.m2Active};
  }
`

const Wrapper = styled.div`
  position: relative;
`

const StyledSelect = styled.select`
  appearance: none;
  color: ${colors.grayscale.g100};
  background-color: ${colors.grayscale.g0};
  display: block;
  font-size: 1rem;
  width: 100%;
  padding: 8px 30px 8px 12px;
  border: 1px solid ${colors.grayscale.g70};
  border-radius: 2px;
  box-shadow: none;
`

const Icon = styled(FontAwesomeIcon)`
  pointer-events: none;
  position: absolute;
  font-size: 1rem;
  right: 12px;
  top: 12px;
`

export const Select = React.memo(GenericSelect) as typeof GenericSelect
