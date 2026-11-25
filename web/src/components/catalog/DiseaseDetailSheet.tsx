'use client';

import React from 'react';
import { useDiseaseDetailQuery } from '@/lib/api/queries';
import { findFallbackDiseaseRecord } from '@/lib/data/fallbackCatalog';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';

export function DiseaseDetailSheet({
  diseaseCode,
}: {
  diseaseCode: string;
}): React.JSX.Element {
  const { t, localizeTreatmentType, localizeDiseaseDetail } = useLanguage();
  const detailQuery = useDiseaseDetailQuery(diseaseCode);
  const localRef = findFallbackDiseaseRecord(diseaseCode);

  if (detailQuery.isLoading && !localRef) {
    return (
      <div
        className="px-5 sm:px-7 py-6 border-t text-sm"
        style={{
          backgroundColor: SEEDHOUSE_COLORS.sheet,
          borderColor: SEEDHOUSE_COLORS.hairline,
          color: SEEDHOUSE_COLORS.inkMuted,
        }}
      >
        {t.sheetLoading}
      </div>
    );
  }

  const detail = detailQuery.data;
  const scientificName = detail?.scientificName ?? localRef?.scientificName ?? '';
  const {
    symptoms: symptomsText,
    description: conditionsText,
    treatments,
  } = localizeDiseaseDetail(diseaseCode, detail, localRef);

  return (
    <div
      className="px-5 sm:px-7 py-6 border-t space-y-5"
      style={{
        backgroundColor: SEEDHOUSE_COLORS.sheet,
        borderColor: SEEDHOUSE_COLORS.hairline,
        color: SEEDHOUSE_COLORS.ink,
      }}
    >
      {scientificName && (
        <div className="text-sm" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
          {t.scientificNameLabel}{' '}
          <em className="font-semibold" style={{ color: SEEDHOUSE_COLORS.ink }}>
            {scientificName}
          </em>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        <div
          className="lg:col-span-4 lg:pr-6 lg:border-r space-y-2"
          style={{ borderColor: '#D6C8B0' }}
        >
          <h4
            className="text-xs font-bold uppercase tracking-[0.12em] m-0"
            style={{ color: SEEDHOUSE_COLORS.olive }}
          >
            {t.sheetSymptomsHeading}
          </h4>
          <p className="text-base leading-relaxed m-0">{symptomsText}</p>
        </div>

        <div
          className="lg:col-span-4 lg:pr-6 lg:border-r space-y-2"
          style={{ borderColor: '#D6C8B0' }}
        >
          <h4
            className="text-xs font-bold uppercase tracking-[0.12em] m-0"
            style={{ color: SEEDHOUSE_COLORS.terracotta }}
          >
            {t.sheetConditionsHeading}
          </h4>
          <p className="text-base leading-relaxed m-0">{conditionsText}</p>
        </div>

        <div className="lg:col-span-4 space-y-3">
          <h4
            className="text-xs font-bold uppercase tracking-[0.12em] m-0"
            style={{ color: SEEDHOUSE_COLORS.moderate }}
          >
            {t.sheetTreatmentsHeading}
          </h4>
          <div className="space-y-3">
            {treatments.map((tr) => (
              <div key={tr.id} className="space-y-1">
                <div className="text-sm font-bold">
                  [{localizeTreatmentType(tr.treatmentType)}] {tr.title}
                </div>
                <p className="text-sm leading-relaxed m-0" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
                  {tr.instructions}
                </p>
                {tr.activeIngredients && (
                  <div className="text-xs leading-snug" style={{ color: SEEDHOUSE_COLORS.ink }}>
                    <span className="font-bold">{t.activeIngredientLabel}</span>{' '}
                    <span>{tr.activeIngredients}</span>
                  </div>
                )}
                {tr.dosageRecommendation && (
                  <div className="text-xs leading-snug" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
                    <span className="font-bold" style={{ color: SEEDHOUSE_COLORS.ink }}>
                      {t.dosageLabel}
                    </span>{' '}
                    <span>{tr.dosageRecommendation}</span>
                  </div>
                )}
                {tr.safetyIntervalDays != null && tr.safetyIntervalDays > 0 && (
                  <div
                    className="text-xs font-bold tabular-nums"
                    style={{ color: SEEDHOUSE_COLORS.critical }}
                  >
                    {t.harvestWaitDays(tr.safetyIntervalDays)}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
