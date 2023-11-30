import styled from 'styled-components'

import { colors } from '../theme'

export const ButtonLink = styled.button`
  color: ${colors.main.m2};
  cursor: pointer;
  text-decoration: underline;
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  display: inline;
  text-align: left;

  &:hover {
    text-decoration: none;
    color: ${colors.main.m1};
  }
`
