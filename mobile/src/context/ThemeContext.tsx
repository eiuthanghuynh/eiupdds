import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { Appearance, ColorSchemeName, Platform } from 'react-native';

import { ColorPalette, Colors } from '@/constants/theme';

export type ThemeMode = 'light' | 'dark';

export interface ThemeContextType {
  mode: ThemeMode;
  isDark: boolean;
  colors: ColorPalette;
  setMode: (mode: ThemeMode) => void;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | null>(null);

const STORAGE_KEY = 'eiu_pdds_theme_mode';

function applyWebTheme(mode: ThemeMode) {
  if (Platform.OS === 'web' && typeof window !== 'undefined') {
    try {
      window.localStorage?.setItem(STORAGE_KEY, mode);
      const root = document.documentElement;
      if (root) {
        root.classList.remove('light', 'dark');
        root.classList.add(mode);
        root.style.colorScheme = mode;
      }
    } catch {
      // ignore
    }
  }
}

function getInitialMode(): ThemeMode {
  if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
    try {
      const saved = window.localStorage.getItem(STORAGE_KEY);
      if (saved === 'light' || saved === 'dark') {
        return saved;
      }
    } catch {
      // ignore storage access errors
    }
  }

  const current = Appearance.getColorScheme();
  return current === 'dark' ? 'dark' : 'light';
}

export function AppThemeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setModeState] = useState<ThemeMode>(getInitialMode);

  const setMode = useCallback((newMode: ThemeMode) => {
    setModeState(newMode);

    try {
      Appearance.setColorScheme(newMode as ColorSchemeName);
    } catch {
      // ignore native appearance errors
    }

    applyWebTheme(newMode);
  }, []);

  const toggleTheme = useCallback(() => {
    setModeState((prev) => {
      const next = prev === 'light' ? 'dark' : 'light';
      try {
        Appearance.setColorScheme(next as ColorSchemeName);
      } catch {
        // ignore
      }
      applyWebTheme(next);
      return next;
    });
  }, []);

  useEffect(() => {
    try {
      Appearance.setColorScheme(mode as ColorSchemeName);
    } catch {
      // ignore
    }

    applyWebTheme(mode);
  }, [mode]);

  const value = useMemo(
    () => ({
      mode,
      isDark: mode === 'dark',
      colors: Colors[mode],
      setMode,
      toggleTheme,
    }),
    [mode, setMode, toggleTheme]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useAppTheme(): ThemeContextType {
  const context = useContext(ThemeContext);
  if (!context) {
    const current = Appearance.getColorScheme();
    const mode: ThemeMode = current === 'dark' ? 'dark' : 'light';
    return {
      mode,
      isDark: mode === 'dark',
      colors: Colors[mode],
      setMode: () => {},
      toggleTheme: () => {},
    };
  }
  return context;
}
