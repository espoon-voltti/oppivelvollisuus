import styled from 'styled-components'

import { colors } from './theme'

export const FlexCol = styled.div`
  display: flex;
  flex-direction: column;
`

export const FlexRow = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
`

export const FlexColWithGaps = styled(FlexCol)<{
  $gapSize?: 's' | 'm' | 'L' | 'XL'
}>`
  > * {
    margin-bottom: ${(p) =>
      p.$gapSize === 'XL'
        ? '64px'
        : p.$gapSize === 'L'
          ? '32px'
          : p.$gapSize === 'm'
            ? '16px'
            : '8px'};

    &:last-child {
      margin-bottom: 0;
    }
  }
`

export const FlexRowWithGaps = styled(FlexRow)<{
  $gapSize?: 's' | 'm' | 'L' | 'XL'
}>`
  > * {
    margin-right: ${(p) =>
      p.$gapSize === 'XL'
        ? '64px'
        : p.$gapSize === 'L'
          ? '32px'
          : p.$gapSize === 'm'
            ? '16px'
            : '8px'};

    &:last-child {
      margin-right: 0;
    }
  }
`

export const FlexLeftRight = styled(FlexRow)`
  justify-content: space-between;
  align-items: center;
`

export const FlexRight = styled(FlexRow)`
  justify-content: flex-end;
  align-items: center;
`

export const VerticalGap = styled.div<{ $size?: 's' | 'm' | 'L' }>`
  height: ${(p) =>
    p.$size === 'L' ? '32px' : p.$size === 'm' ? '16px' : '8px'};
`

export const Table = styled.table`
  border-collapse: collapse;

  tr {
    border-bottom: 1px solid #888;
    padding: 8px;
  }

  td, th {
    padding: 8px;
  }

`

export const PageContainer = styled.div`
  max-width: 1024px;
  margin: 0 auto;
`

export const SectionContainer = styled.div<{ $minHeight?: string }>`
  padding: 16px;
  background-color: #fff;
  ${(p) => (p.$minHeight ? `min-height: ${p.$minHeight};` : '')}
`

export const BottomActionBar = styled.div`
  padding: 16px;
  background-color: #fff;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  margin-top: 16px;
`
export const LabeledInputS = styled(FlexColWithGaps)`
  width: 128px;
`
export const LabeledInputM = styled(FlexColWithGaps)`
  width: 256px;
`
export const LabeledInputL = styled(FlexColWithGaps)`
  width: 400px;
`
export const LabeledInputFull = styled(FlexColWithGaps)`
  flex-grow: 1;
`
export const RowOfInputs = styled(FlexRowWithGaps)`
  align-items: flex-start;
  flex-grow: 1;
`

export const GroupOfInputRows = styled(FlexColWithGaps)`
  flex-grow: 1;
`

export const Separator = styled.div`
  border-top: 1px solid ${colors.grayscale.g15};
  margin: 16px 0;
  width: 100%;
`
