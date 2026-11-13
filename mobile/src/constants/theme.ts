/**
 * Below are the colors that are used in the app. The colors are defined in the light and dark mode.
 * There are many other ways to style your app. For example, [Nativewind](https://www.nativewind.dev/), [Tamagui](https://tamagui.dev/), [unistyles](https://reactnativeunistyles.vercel.app), etc.
 */

import '@/global.css';

import { Platform } from 'react-native';

export const Palette = {
  primary: '#CFFF74',
  primaryContent: '#2F3A10',
  secondary: '#2F3A10',
  secondaryContent: '#F7F8F1',
  background: '#F7F8F1',
  surface: '#E7EBD9',
  surfaceElevated: '#FFFFFF',
  text: '#2F3A10',
  textMuted: '#5A6640',
  border: '#D5DEC2',
  white: '#ffffff',
  darkBackground: '#111609',
  darkSurface: '#1C2410',
  darkSurfaceElevated: '#263016',
  darkText: '#F7F8F1',
  darkTextMuted: '#A3B18A',
  darkBorder: '#2F3A10',
} as const;

export const Colors = {
  light: {
    text: '#2F3A10',
    background: '#F7F8F1',
    surface: '#E7EBD9',
    surfaceElevated: '#FFFFFF',
    backgroundElement: '#E7EBD9',
    backgroundSelected: '#2F3A10',
    navBackground: '#CFFF74',
    textSecondary: '#5A6640',
    textMuted: '#5A6640',
    textOnSecondary: '#ffffff',
    primaryContent: '#2F3A10',
    border: '#D5DEC2',
    borderSubtle: '#D5DEC2',
    borderStrong: '#2F3A10',
    card: '#E7EBD9',
    primary: '#CFFF74',
    secondary: '#2F3A10',
    success: '#2E7D32',
    warning: '#D97706',
    error: '#C62828',
    info: '#1565C0',
  },
  dark: {
    text: '#F7F8F1',
    background: '#111609',
    surface: '#1C2410',
    surfaceElevated: '#263016',
    backgroundElement: '#1C2410',
    backgroundSelected: '#2F3A10',
    navBackground: '#1C2410',
    textSecondary: '#A3B18A',
    textMuted: '#A3B18A',
    textOnSecondary: '#ffffff',
    primaryContent: '#111609',
    border: '#2F3A10',
    borderSubtle: '#2F3A10',
    borderStrong: '#5A6640',
    card: '#1C2410',
    primary: '#CFFF74',
    secondary: '#2F3A10',
    success: '#66BB6A',
    warning: '#FBBF24',
    error: '#EF5350',
    info: '#42A5F5',
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

export type ColorPalette = {
  readonly [K in ThemeColor]: string;
};

export const Fonts = Platform.select({
  ios: {
    /** iOS `UIFontDescriptorSystemDesignDefault` */
    sans: 'system-ui',
    /** iOS `UIFontDescriptorSystemDesignSerif` */
    serif: 'ui-serif',
    /** iOS `UIFontDescriptorSystemDesignRounded` */
    rounded: 'ui-rounded',
    /** iOS `UIFontDescriptorSystemDesignMonospaced` */
    mono: 'ui-monospace',
  },
  default: {
    sans: 'normal',
    serif: 'serif',
    rounded: 'normal',
    mono: 'monospace',
  },
  web: {
    sans: 'var(--font-display)',
    serif: 'var(--font-serif)',
    rounded: 'var(--font-rounded)',
    mono: 'var(--font-mono)',
  },
});

export const Spacing = {
  half: 2,
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 64,
} as const;

export const BottomTabInset = Platform.select({ ios: 50, android: 80, web: 64 }) ?? 0;
export const MaxContentWidth = 800;
