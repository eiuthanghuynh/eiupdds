/**
 * Below are the colors that are used in the app. The colors are defined in the light and dark mode.
 * There are many other ways to style your app. For example, [Nativewind](https://www.nativewind.dev/), [Tamagui](https://tamagui.dev/), [unistyles](https://reactnativeunistyles.vercel.app), etc.
 */

import '@/global.css';

import { Platform } from 'react-native';

export const Palette = {
  primary: '#CFFF74',
  secondary: '#2F3A10',
  background: '#F7F8F1',
  surface: '#E7EBD9',
  text: '#2F3A10',
  textOnSecondary: '#ffffff',
  white: '#ffffff',
  darkBackground: '#0E1308',
  darkSurface: '#18200E',
} as const;

export const Colors = {
  light: {
    text: '#2F3A10',
    background: '#F7F8F1',
    surface: '#E7EBD9',
    backgroundElement: '#E7EBD9',
    backgroundSelected: '#2F3A10',
    navBackground: '#CFFF74',
    textSecondary: 'rgba(47, 58, 16, 0.72)',
    textOnSecondary: '#ffffff',
    border: 'rgba(47, 58, 16, 0.15)',
    card: '#E7EBD9',
    primary: '#CFFF74',
    secondary: '#2F3A10',
  },
  dark: {
    text: '#F7F8F1',
    background: '#0E1308',
    surface: '#18200E',
    backgroundElement: '#18200E',
    backgroundSelected: '#2F3A10',
    navBackground: '#18200E',
    textSecondary: 'rgba(231, 235, 217, 0.72)',
    textOnSecondary: '#ffffff',
    border: 'rgba(207, 255, 116, 0.15)',
    card: '#18200E',
    primary: '#CFFF74',
    secondary: '#2F3A10',
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
