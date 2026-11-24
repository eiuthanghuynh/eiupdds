'use client';

import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import {
  EN_SEVERITY_LABELS,
  EN_TREATMENT_TYPES,
  type Locale,
  UI_DICTIONARY,
  type UiCopy,
  VI_DISEASE_NAMES,
  VI_PLANT_NAMES,
  VI_SEVERITY_LABELS,
  VI_TREATMENT_TYPES,
} from './dictionary';

const STORAGE_KEY = 'eiu_pdds_lang';

export interface LanguageContextValue {
  locale: Locale;
  setLocale: (next: Locale) => void;
  t: UiCopy;
  localizePlantName: (code: string, fallback: string) => string;
  localizeDiseaseName: (code: string, fallback: string) => string;
  localizeSeverity: (severity?: string | null) => string;
  localizeTreatmentType: (type: string) => string;
}

const LanguageContext = createContext<LanguageContextValue | null>(null);

export function LanguageProvider({ children }: { children: React.ReactNode }): React.JSX.Element {
  const [locale, setLocaleState] = useState<Locale>('vi');

  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(STORAGE_KEY);
      if (saved === 'vi' || saved === 'en') {
        setLocaleState(saved);
      }
    } catch {
      // Ignore storage failures in restricted browsers
    }
  }, []);

  const setLocale = useCallback((next: Locale) => {
    setLocaleState(next);
    try {
      window.localStorage.setItem(STORAGE_KEY, next);
      document.documentElement.lang = next;
    } catch {
      // Ignore storage failures
    }
  }, []);

  const localizePlantName = useCallback(
    (code: string, fallback: string): string => {
      const upper = code?.toUpperCase() ?? '';
      if (locale === 'vi' && VI_PLANT_NAMES[upper]) {
        return VI_PLANT_NAMES[upper];
      }
      return fallback || upper;
    },
    [locale]
  );

  const localizeDiseaseName = useCallback(
    (code: string, fallback: string): string => {
      const upper = code?.toUpperCase() ?? '';
      if (locale === 'vi') {
        if (VI_DISEASE_NAMES[upper]) {
          return VI_DISEASE_NAMES[upper];
        }
        if (upper.endsWith('_HEALTHY')) {
          return 'Lá cây khỏe mạnh';
        }
      }
      return fallback || upper;
    },
    [locale]
  );

  const localizeSeverity = useCallback(
    (severity?: string | null): string => {
      if (!severity) {
        return locale === 'vi' ? 'Lá khỏe mạnh' : 'Healthy leaf';
      }
      const upper = severity.toUpperCase();
      return locale === 'vi'
        ? VI_SEVERITY_LABELS[upper] ?? severity
        : EN_SEVERITY_LABELS[upper] ?? severity;
    },
    [locale]
  );

  const localizeTreatmentType = useCallback(
    (type: string): string => {
      const upper = type?.toUpperCase() ?? '';
      return locale === 'vi'
        ? VI_TREATMENT_TYPES[upper] ?? type
        : EN_TREATMENT_TYPES[upper] ?? type;
    },
    [locale]
  );

  const value = useMemo<LanguageContextValue>(
    () => ({
      locale,
      setLocale,
      t: UI_DICTIONARY[locale],
      localizePlantName,
      localizeDiseaseName,
      localizeSeverity,
      localizeTreatmentType,
    }),
    [locale, setLocale, localizePlantName, localizeDiseaseName, localizeSeverity, localizeTreatmentType]
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useLanguage(): LanguageContextValue {
  const ctx = useContext(LanguageContext);
  if (!ctx) {
    throw new Error('useLanguage must be used inside LanguageProvider');
  }
  return ctx;
}
