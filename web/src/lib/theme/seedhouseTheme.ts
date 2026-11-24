import type { ThemeConfig } from 'antd';

export const SEEDHOUSE_COLORS = {
  kraft: '#E6DAC4',
  parchment: '#EFE7D6',
  sheet: '#FAF5EB',
  olive: '#434C28',
  oliveStage: '#3D4624',
  oliveMat: '#2F371B',
  oliveRule: '#697349',
  terracotta: '#8C3419',
  terracottaHover: '#732912',
  mustard: '#F5CF7A',
  critical: '#8F1D1E',
  moderate: '#78350F',
  hairline: '#B8A88A',
  ink: '#241C14',
  inkMuted: '#4A3D2E',
  cream: '#F6EFE0',
  creamWarm: '#FAF3E6',
} as const;

export const seedhouseAntdTheme: ThemeConfig = {
  cssVar: true,
  hashed: false,
  token: {
    colorPrimary: SEEDHOUSE_COLORS.terracotta,
    colorPrimaryHover: SEEDHOUSE_COLORS.terracottaHover,
    colorSuccess: SEEDHOUSE_COLORS.olive,
    colorWarning: SEEDHOUSE_COLORS.moderate,
    colorError: SEEDHOUSE_COLORS.critical,
    colorInfo: SEEDHOUSE_COLORS.olive,
    colorBgBase: SEEDHOUSE_COLORS.parchment,
    colorBgContainer: SEEDHOUSE_COLORS.sheet,
    colorBgElevated: SEEDHOUSE_COLORS.sheet,
    colorTextBase: SEEDHOUSE_COLORS.ink,
    colorTextSecondary: SEEDHOUSE_COLORS.inkMuted,
    colorTextPlaceholder: '#5C4D3C',
    colorBorder: SEEDHOUSE_COLORS.hairline,
    colorBorderSecondary: '#CFC1A8',
    borderRadius: 2,
    fontSize: 16,
    fontFamily: 'var(--font-body), system-ui, -apple-system, sans-serif',
    controlHeight: 44,
  },
  components: {
    Button: {
      fontWeight: 700,
      borderRadius: 2,
      controlHeightLG: 52,
    },
    Input: {
      activeBorderColor: SEEDHOUSE_COLORS.terracotta,
      hoverBorderColor: SEEDHOUSE_COLORS.olive,
      borderRadius: 2,
    },
    Select: {
      borderRadius: 2,
      optionSelectedBg: '#E6DAC4',
    },
  },
};
