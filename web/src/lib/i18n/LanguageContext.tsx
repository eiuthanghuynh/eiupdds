'use client';

import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type {
  DiagnosisAnalyzeResponse,
  DiseaseDetailDto,
  SampleSpecimen,
  TreatmentDto,
} from '@/types/pdds';
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
import {
  getDiseaseDetailOverride,
  localizeSampleResponse as localizeSpecimenPayload,
  VI_SAMPLE_SPECIMENS,
} from './viCatalog';

const STORAGE_KEY = 'eiu_pdds_lang';

export interface LanguageContextValue {
  locale: Locale;
  setLocale: (next: Locale) => void;
  t: UiCopy;
  localizePlantName: (code: string, fallback: string) => string;
  localizeDiseaseName: (code: string, fallback: string) => string;
  localizeSeverity: (severity?: string | null) => string;
  localizeTreatmentType: (type: string) => string;
  localizeSampleLabel: (sample: SampleSpecimen) => string;
  localizeSampleResponse: (sample: SampleSpecimen) => DiagnosisAnalyzeResponse;
  localizeDiseaseDetail: (
    diseaseCode: string,
    detail?: DiseaseDetailDto,
    englishFallback?: DiseaseDetailDto
  ) => {
    symptoms: string;
    description: string;
    treatments: TreatmentDto[];
  };
}

const LanguageContext = createContext<LanguageContextValue | null>(null);

export function LanguageProvider({ children }: { children: React.ReactNode }): React.JSX.Element {
  const [locale, setLocaleState] = useState<Locale>('vi');

  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(STORAGE_KEY);
      if (saved === 'vi' || saved === 'en') {
        setLocaleState(saved);
        document.documentElement.lang = saved;
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
          return UI_DICTIONARY.vi.healthyLeafTitle;
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

  const localizeSampleLabel = useCallback(
    (sample: SampleSpecimen): string => {
      if (locale === 'vi') {
        return VI_SAMPLE_SPECIMENS[sample.key]?.shortLabel ?? sample.shortLabel;
      }
      return sample.shortLabel;
    },
    [locale]
  );

  const localizeSampleResponse = useCallback(
    (sample: SampleSpecimen): DiagnosisAnalyzeResponse => {
      return localizeSpecimenPayload(sample, locale);
    },
    [locale]
  );

  const localizeDiseaseDetail = useCallback(
    (
      diseaseCode: string,
      detail?: DiseaseDetailDto,
      englishFallback?: DiseaseDetailDto
    ): {
      symptoms: string;
      description: string;
      treatments: TreatmentDto[];
    } => {
      if (locale === 'vi') {
        return getDiseaseDetailOverride(diseaseCode, 'vi', detail ?? englishFallback);
      }
      const primary = detail ?? englishFallback;
      return {
        symptoms: primary?.symptoms ?? englishFallback?.symptoms ?? '',
        description: primary?.description ?? englishFallback?.description ?? '',
        treatments: primary?.treatments?.length
          ? primary.treatments
          : englishFallback?.treatments ?? [],
      };
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
      localizeSampleLabel,
      localizeSampleResponse,
      localizeDiseaseDetail,
    }),
    [
      locale,
      setLocale,
      localizePlantName,
      localizeDiseaseName,
      localizeSeverity,
      localizeTreatmentType,
      localizeSampleLabel,
      localizeSampleResponse,
      localizeDiseaseDetail,
    ]
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
