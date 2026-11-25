'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';
import type { DiagnosisAnalyzeResponse, TreatmentDto, TreatmentType } from '@/types/pdds';

const PROTOCOL_ORDER: Record<TreatmentType, number> = {
  BIOLOGICAL: 0,
  CULTURAL: 1,
  PREVENTIVE: 2,
  CHEMICAL: 3,
};

export function PrescriptionSpread({
  diagnosis,
}: {
  diagnosis: DiagnosisAnalyzeResponse;
}): React.JSX.Element {
  const { t, localizeDiseaseName, localizeTreatmentType, localizeTreatments } = useLanguage();
  const [selectedIndex, setSelectedIndex] = useState(0);

  const sortedTreatments = useMemo<TreatmentDto[]>(() => {
    const raw = localizeTreatments(
      diagnosis.dominantDisease?.diseaseCode,
      diagnosis.recommendedTreatments ?? []
    );
    return [...raw].sort(
      (a, b) =>
        (PROTOCOL_ORDER[a.treatmentType] ?? 99) - (PROTOCOL_ORDER[b.treatmentType] ?? 99)
    );
  }, [diagnosis.dominantDisease?.diseaseCode, diagnosis.recommendedTreatments, localizeTreatments]);

  useEffect(() => {
    setSelectedIndex(0);
  }, [diagnosis.diagnosisId]);

  const activeTreatment = sortedTreatments[selectedIndex] ?? sortedTreatments[0] ?? null;
  const waitDays =
    activeTreatment?.safetyIntervalDays != null && activeTreatment.safetyIntervalDays > 0
      ? activeTreatment.safetyIntervalDays
      : 0;
  const maxChemicalWaitDays = useMemo(
    () =>
      Math.max(
        0,
        ...sortedTreatments
          .filter((tr) => tr.treatmentType === 'CHEMICAL')
          .map((tr) => tr.safetyIntervalDays ?? 0)
      ),
    [sortedTreatments]
  );
  const isChemicalOrBio =
    activeTreatment?.treatmentType === 'BIOLOGICAL' ||
    activeTreatment?.treatmentType === 'CHEMICAL';

  return (
    <section
      aria-label={t.prescriptionHeading}
      className="w-full px-5 sm:px-8 lg:px-12 py-8 border-t border-b"
      style={{
        backgroundColor: SEEDHOUSE_COLORS.parchment,
        borderColor: SEEDHOUSE_COLORS.hairline,
        color: SEEDHOUSE_COLORS.ink,
      }}
    >
      <div className="max-w-7xl mx-auto space-y-6">
        <div
          className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b"
          style={{ borderColor: '#CFC0A5' }}
        >
          <h2 className="font-display text-xl sm:text-2xl font-bold m-0">
            {t.prescriptionHeading}
          </h2>

          {sortedTreatments.length > 0 && (
            <div role="tablist" aria-label={t.prescriptionHeading} className="flex flex-wrap gap-2">
              {sortedTreatments.map((tr, idx) => {
                const isSelected = idx === selectedIndex;
                return (
                  <button
                    key={tr.id}
                    type="button"
                    role="tab"
                    aria-selected={isSelected}
                    onClick={() => setSelectedIndex(idx)}
                    className="px-4 py-2.5 text-xs sm:text-sm font-bold uppercase tracking-[0.08em] border cursor-pointer transition-colors"
                    style={
                      isSelected
                        ? {
                            backgroundColor:
                              tr.treatmentType === 'CHEMICAL'
                                ? SEEDHOUSE_COLORS.terracotta
                                : SEEDHOUSE_COLORS.olive,
                            borderColor:
                              tr.treatmentType === 'CHEMICAL'
                                ? SEEDHOUSE_COLORS.terracotta
                                : SEEDHOUSE_COLORS.olive,
                            color: '#FFFDF9',
                          }
                        : {
                            backgroundColor: SEEDHOUSE_COLORS.sheet,
                            borderColor: SEEDHOUSE_COLORS.hairline,
                            color: SEEDHOUSE_COLORS.ink,
                          }
                    }
                  >
                    {localizeTreatmentType(tr.treatmentType)}
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {activeTreatment && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Column 1: Field Action Steps */}
            <div
              className="lg:col-span-5 lg:pr-8 lg:border-r space-y-2.5"
              style={{ borderColor: SEEDHOUSE_COLORS.hairline }}
            >
              <h3
                className="text-sm font-bold uppercase tracking-[0.12em] m-0"
                style={{ color: SEEDHOUSE_COLORS.olive }}
              >
                {t.colInstructions}
              </h3>
              <div className="font-display text-lg font-bold leading-snug">
                {activeTreatment.title}
              </div>
              <p className="text-base leading-relaxed m-0 max-w-[65ch]" style={{ color: SEEDHOUSE_COLORS.ink }}>
                {activeTreatment.instructions}
              </p>
            </div>

            {/* Column 2: Active Ingredient, Dosage & Co-Occurring Signs */}
            <div
              className="lg:col-span-4 lg:pr-8 lg:border-r space-y-3"
              style={{ borderColor: SEEDHOUSE_COLORS.hairline }}
            >
              <h3
                className="text-sm font-bold uppercase tracking-[0.12em] m-0"
                style={{ color: SEEDHOUSE_COLORS.terracotta }}
              >
                {t.colDosageAndCoInfection}
              </h3>

              {activeTreatment.activeIngredients ? (
                <p className="text-base leading-relaxed m-0">
                  <strong>{t.activeIngredientLabel}</strong> {activeTreatment.activeIngredients}
                </p>
              ) : (
                <p className="text-base leading-relaxed m-0" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
                  {t.noChemicalNote}
                </p>
              )}

              {activeTreatment.dosageRecommendation && (
                <p className="text-base leading-relaxed m-0">
                  <strong>{isChemicalOrBio ? t.dosageLabel : t.culturalNoteLabel}</strong>{' '}
                  {activeTreatment.dosageRecommendation}
                </p>
              )}

              {diagnosis.coOccurringDiseases && diagnosis.coOccurringDiseases.length > 0 && (
                <p className="text-sm leading-relaxed m-0 pt-2 border-t" style={{ borderColor: '#D6C8B0' }}>
                  <strong>{t.coInfectionPrefix}</strong>{' '}
                  {diagnosis.coOccurringDiseases
                    .map((d) => localizeDiseaseName(d.diseaseCode, d.commonName))
                    .join(', ')}
                </p>
              )}
            </div>

            {/* Column 3: Pre-Harvest Safety Interval & Woodcut Day Seal */}
            <div className="lg:col-span-3 flex items-center justify-between gap-4">
              <div className="space-y-2">
                <h3
                  className="text-sm font-bold uppercase tracking-[0.12em] m-0"
                  style={{
                    color: waitDays > 0 ? SEEDHOUSE_COLORS.critical : SEEDHOUSE_COLORS.olive,
                  }}
                >
                  {t.colHarvestSafety}
                </h3>
                <p className="text-base font-semibold leading-relaxed m-0">
                  {waitDays > 0 ? t.harvestWaitDays(waitDays) : t.harvestSafeZero}
                </p>
                {activeTreatment.warning && (
                  <p className="text-sm leading-relaxed m-0" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
                    {activeTreatment.warning}
                  </p>
                )}
                {waitDays === 0 && maxChemicalWaitDays > 0 && (
                  <p
                    className="text-xs font-semibold leading-relaxed m-0"
                    style={{ color: SEEDHOUSE_COLORS.critical }}
                  >
                    {t.chemicalWaitCrossNotice(maxChemicalWaitDays)}
                  </p>
                )}
              </div>

              <div
                className="shrink-0 w-20 h-20 rounded-full border-2 border-dashed flex flex-col items-center justify-center text-center p-2 tabular-nums"
                style={{
                  borderColor:
                    waitDays > 0 ? SEEDHOUSE_COLORS.critical : SEEDHOUSE_COLORS.olive,
                  color: waitDays > 0 ? SEEDHOUSE_COLORS.critical : SEEDHOUSE_COLORS.olive,
                }}
              >
                <span className="text-xl font-extrabold leading-none">
                  {waitDays > 0 ? String(waitDays).padStart(2, '0') : '0'}
                </span>
                <span className="text-xs font-bold uppercase mt-1">
                  {t.harvestDayUnit}
                </span>
              </div>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
