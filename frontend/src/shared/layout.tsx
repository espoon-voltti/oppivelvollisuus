import styled from 'styled-components'

export const FlexCol = styled.div`
  display: flex;
  flex-direction: column;
`

export const FlexRow = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
`

export const FlexColWithGaps = styled(FlexCol)<{ $gapSize?: 's' | 'm' | 'L' }>`
  > * {
    margin-bottom: ${(p) =>
      p.$gapSize === 'L' ? '32px' : p.$gapSize === 'm' ? '16px' : '8px'};

    &:last-child {
      margin-bottom: 0;
    }
  }
`

export const FlexRowWithGaps = styled(FlexRow)<{ $gapSize?: 's' | 'm' | 'L' }>`
  > * {
    margin-right: ${(p) =>
      p.$gapSize === 'L' ? '32px' : p.$gapSize === 'm' ? '16px' : '8px'};

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
  td {
    border-top: 1px solid #888;
    border-right: 1px solid #888;
    padding: 8px;
  }

  td:last-child {
    border-right: none;
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
