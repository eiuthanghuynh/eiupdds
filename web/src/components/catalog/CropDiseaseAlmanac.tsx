'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { Input } from 'antd';
import { useDiseasesQuery, usePlantsQuery } from '@/lib/api/queries';
import { isHealthyLeafDisease, matchesDiseaseKeyword } from '@/lib/data/fallbackCatalog';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';
import {
  ChevronFoldIcon,
  CropGlyphIcon,
  SearchLoupeIcon,
} from '@/components/primitives/WoodcutIcons';
import { DiseaseDetailSheet } from '@/components/catalog/DiseaseDetailSheet';

const HEALTHY_ONLY_PLANTS = new Set([
  'PEACH',
  'SOYBEAN',
  'BLUEBERRY',
  'CHERRY',
  'RASPBERRY',
  'SQUASH',
  'STRAWBERRY',
]);
const DEFAULT_VISIBLE_ROWS = 6;

export function CropDiseaseAlmanac(): React.JSX.Element {
  const { t, localizePlantName, localizeDiseaseName, localizeSeverity } = useLanguage();
  const [selectedPlantCode, setSelectedPlantCode] = useState<string>('');
  const [keyword, setKeyword] = useState<string>('');
  const [expandedDiseaseCode, setExpandedDiseaseCode] = useState<string | null>(null);
  const [showAll, setShowAll] = useState<boolean>(false);

  const plantsQuery = usePlantsQuery();
  const diseasesQuery = useDiseasesQuery({
    plantCode: selectedPlantCode || undefined,
  });

  useEffect(() => {
    if (!expandedDiseaseCode) {
      return;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setExpandedDiseaseCode(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [expandedDiseaseCode]);

  const allPlants = plantsQuery.data?.plants ?? [];
  const plants = allPlants.filter(
    (p) => !HEALTHY_ONLY_PLANTS.has(p.plantCode.toUpperCase())
  );
  const healthyOnlyPlants = allPlants.filter((p) =>
    HEALTHY_ONLY_PLANTS.has(p.plantCode.toUpperCase())
  );
  const allDiseases = (diseasesQuery.data?.diseases ?? []).filter(
    (disease) => !isHealthyLeafDisease(disease)
  );
  const diseases = useMemo(
    () =>
      allDiseases.filter((disease) =>
        matchesDiseaseKeyword(
          disease,
          keyword,
          selectedPlantCode ? [selectedPlantCode] : undefined
        )
      ),
    [allDiseases, keyword, selectedPlantCode]
  );
  const isUnfiltered = !selectedPlantCode && !keyword.trim();
  const visibleDiseases =
    isUnfiltered && !showAll ? diseases.slice(0, DEFAULT_VISIBLE_ROWS) : diseases;
  const isOffline = Boolean(diseasesQuery.data?.isOfflineFallback);

  return (
    <section
      id="almanac"
      className="w-full px-5 sm:px-8 lg:px-12 py-10 scroll-mt-2 min-h-[calc(100vh-96px)]"
      style={{
        backgroundColor: SEEDHOUSE_COLORS.parchment,
        color: SEEDHOUSE_COLORS.ink,
      }}
    >
      <div className="max-w-7xl mx-auto w-full space-y-6">
        <div
          className="pb-4 border-b flex flex-wrap items-end justify-between gap-4"
          style={{ borderColor: SEEDHOUSE_COLORS.hairline }}
        >
          <div className="space-y-1.5 max-w-3xl">
            <h2 className="font-display text-2xl sm:text-3xl font-bold m-0">
              {t.almanacHeading}
            </h2>
            <p className="text-base leading-relaxed m-0" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
              {t.almanacLead}
            </p>
          </div>

          {isOffline && (
            <span className="text-xs font-semibold" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
              {t.almanacOfflineNote}
            </span>
          )}
        </div>

        {/* Crop Filter Strip + Keyword Input */}
        <div className="space-y-3">
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => {
                setSelectedPlantCode('');
                setExpandedDiseaseCode(null);
              }}
              aria-pressed={selectedPlantCode === ''}
              className="px-4 py-2 min-h-[44px] text-xs sm:text-sm font-bold border cursor-pointer transition-all duration-200 ease-out active:translate-y-[1px]"
              style={
                selectedPlantCode === ''
                  ? {
                      backgroundColor: SEEDHOUSE_COLORS.olive,
                      borderColor: SEEDHOUSE_COLORS.olive,
                      color: '#FAF3E6',
                    }
                  : {
                      backgroundColor: SEEDHOUSE_COLORS.sheet,
                      borderColor: SEEDHOUSE_COLORS.hairline,
                      color: SEEDHOUSE_COLORS.ink,
                    }
              }
            >
              {t.almanacAllCrops}
            </button>

            {plants.map((plant) => {
              const active = selectedPlantCode === plant.plantCode;
              return (
                <button
                  key={plant.id}
                  type="button"
                  onClick={() => {
                    setSelectedPlantCode(plant.plantCode);
                    setExpandedDiseaseCode(null);
                  }}
                  aria-pressed={active}
                  className="px-3.5 py-2 min-h-[44px] text-xs sm:text-sm font-bold border flex items-center gap-2 cursor-pointer transition-all duration-200 ease-out active:translate-y-[1px]"
                  style={
                    active
                      ? {
                          backgroundColor: SEEDHOUSE_COLORS.terracotta,
                          borderColor: SEEDHOUSE_COLORS.terracotta,
                          color: '#FFFDF9',
                        }
                      : {
                          backgroundColor: SEEDHOUSE_COLORS.sheet,
                          borderColor: SEEDHOUSE_COLORS.hairline,
                          color: SEEDHOUSE_COLORS.ink,
                        }
                  }
                >
                  <CropGlyphIcon
                    size={16}
                    stroke={active ? '#FFFDF9' : SEEDHOUSE_COLORS.olive}
                  />
                  <span>{localizePlantName(plant.plantCode, plant.commonName)}</span>
                </button>
              );
            })}
          </div>

          {healthyOnlyPlants.length > 0 && (
            <p className="text-xs m-0" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
              {t.almanacHealthyCropsNote(
                healthyOnlyPlants
                  .map((p) => localizePlantName(p.plantCode, p.commonName))
                  .join(', ')
              )}
            </p>
          )}

          <div className="max-w-xl">
            <Input
              size="large"
              allowClear
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder={t.almanacSearchPlaceholder}
              prefix={<SearchLoupeIcon size={16} stroke={SEEDHOUSE_COLORS.inkMuted} />}
              aria-label={t.almanacSearchPlaceholder}
            />
          </div>
        </div>

        {/* Hairline-Divided Disease Ledger with Inline Fold-Open Sheets */}
        <div
          className="border-t border-b divide-y min-h-[380px]"
          style={{ borderColor: SEEDHOUSE_COLORS.hairline }}
        >
          {diseasesQuery.isLoading && diseases.length === 0 ? (
            <div className="py-8 text-base" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
              ...
            </div>
          ) : visibleDiseases.length === 0 ? (
            <div className="py-8 text-base" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
              {t.almanacEmpty}
            </div>
          ) : (
            visibleDiseases.map((disease) => {
              const isOpen = expandedDiseaseCode === disease.diseaseCode;
              const isSevere =
                disease.severity === 'CRITICAL' || disease.severity === 'HIGH';

              return (
                <div
                  key={disease.id}
                  style={{ borderColor: '#CFC0A5' }}
                >
                  <button
                    type="button"
                    data-testid="almanac-disease-row"
                    aria-expanded={isOpen}
                    onClick={() =>
                      setExpandedDiseaseCode((prev) =>
                        prev === disease.diseaseCode ? null : disease.diseaseCode
                      )
                    }
                    className="w-full min-h-[44px] px-4 sm:px-6 py-3.5 text-left flex flex-wrap items-center justify-between gap-4 cursor-pointer transition-colors duration-200 hover:bg-[#FAF5EB]"
                    style={{
                      backgroundColor: isOpen ? SEEDHOUSE_COLORS.sheet : 'transparent',
                      color: SEEDHOUSE_COLORS.ink,
                    }}
                  >
                    <div className="font-display text-lg sm:text-xl font-bold leading-snug">
                      {localizeDiseaseName(disease.diseaseCode, disease.commonName)}
                    </div>

                    <div className="flex items-center gap-4 shrink-0">
                      <span
                        className="px-3 py-1 text-xs font-bold uppercase tracking-[0.08em] border"
                        style={
                          isSevere
                            ? {
                                borderColor: SEEDHOUSE_COLORS.critical,
                                color: SEEDHOUSE_COLORS.critical,
                                backgroundColor: '#FCEEEB',
                              }
                            : {
                                borderColor: SEEDHOUSE_COLORS.moderate,
                                color: SEEDHOUSE_COLORS.moderate,
                                backgroundColor: '#FAF2E1',
                              }
                        }
                      >
                        {localizeSeverity(disease.severity)}
                      </span>
                      <ChevronFoldIcon
                        size={18}
                        stroke={SEEDHOUSE_COLORS.ink}
                        open={isOpen}
                      />
                    </div>
                  </button>

                  {isOpen && <DiseaseDetailSheet diseaseCode={disease.diseaseCode} />}
                </div>
              );
            })
          )}
        </div>

        {isUnfiltered && diseases.length > DEFAULT_VISIBLE_ROWS && (
          <div className="flex justify-center pt-1">
            <button
              type="button"
              onClick={() => {
                setShowAll((prev) => !prev);
                if (showAll) {
                  setExpandedDiseaseCode(null);
                }
              }}
              className="px-5 py-2.5 min-h-[44px] text-sm font-bold border cursor-pointer transition-all duration-200 ease-out active:translate-y-[1px] hover:bg-[#FAF5EB]"
              style={{
                backgroundColor: SEEDHOUSE_COLORS.sheet,
                borderColor: SEEDHOUSE_COLORS.hairline,
                color: SEEDHOUSE_COLORS.ink,
              }}
            >
              {showAll ? t.almanacShowLess : t.almanacShowAll(diseases.length)}
            </button>
          </div>
        )}
      </div>
    </section>
  );
}
