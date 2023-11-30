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

export interface DropdownProps<T, E extends HTMLElement> {
  id?: string
  items: readonly T[]
  selectedItem: T | null
  onChange: (item: T | null) => void
  disabled?: boolean
  placeholder?: string
  width?: InputWidth
  getItemLabel?: (item: T) => string
  getItemDataQa?: (item: T) => string | undefined
  name?: string
  onFocus?: FocusEventHandler<E>
  'data-qa'?: string
}

type SelectProps<T> = DropdownProps<T, HTMLSelectElement> &
  (T extends string | number
    ? { getItemValue?: never }
    : { getItemValue: (item: T) => string })

function SelectGeneric<T>(props: SelectProps<T>) {
  const {
    id,
    name,
    'data-qa': dataQa,
    items,
    selectedItem,
    getItemLabel = defaultGetItemLabel,
    getItemDataQa,
    onChange,
    onFocus,
    width,
    placeholder,
    disabled
  } = props

  const getItemValue =
    'getItemValue' in props && props.getItemValue
      ? props.getItemValue
      : defaultGetItemLabel

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

function defaultGetItemLabel<T>(item: T) {
  return String(item)
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

export const Select = React.memo(SelectGeneric) as typeof SelectGeneric
