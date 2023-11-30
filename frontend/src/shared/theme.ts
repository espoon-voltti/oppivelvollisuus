export const tabletMin = '600px'

const blueColors = {
  m1: '#00358a',
  m2: '#0047b6',
  m3: '#4d7fcc',
  m4: '#d9e4f4'
}

export const colors = {
  main: {
    ...blueColors,
    m2Hover: blueColors.m1,
    m2Active: blueColors.m1,
    m2Focus: blueColors.m3
  },
  grayscale: {
    g100: '#091c3b',
    g70: '#536076',
    g35: '#a9b0bb',
    g15: '#dadde2',
    g4: '#f7f7f7',
    g0: '#ffffff'
  },
  status: {
    danger: '#ff4f57',
    warning: '#ff8e31',
    success: '#70c673',
    info: blueColors.m2
  }
}

export interface BaseProps {
  className?: string
  'data-qa'?: string
}

export type IconSize = 's' | 'm' | 'L' | 'XL'
