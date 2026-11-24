'use client';

import React, { useState } from 'react';
import { Input } from 'antd';
import { useDiseasesQuery, usePlantsQuery } from '@/lib/api/queries';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';
import {
  ChevronFoldIcon,
  CropGlyphIcon,
  SearchLoupeIcon,
} from '@/components/primitives/WoodcutIcons';
import { DiseaseDetailSheet } from '@/components/catalog/DiseaseDetailSheet';

export function CropDiseaseAlmanac(): React.JSX.Element {
  const { t, localizePlantName, localizeDiseaseName, localizeSeverity } = useLanguage();
  const [selectedPlantCode, setSelectedPlantCode] = useState<string>('');
  const [keyword, setKeyword] = useState<string>('');
  const [expandedDiseaseCode, setExpandedDiseaseCode] = useState<string | null>(null);

  const plantsQuery = usePlantsQuery();
  const diseasesQuery = useDiseasesQuery({
    plantCode: selectedPlantCode || undefined,
    keyword: keyword || undefined,
  });

  const plants = plantsQuery.data?.plants ?? [];
  const diseases = diseasesQuery.data?.diseases ?? [];
  const isOffline = Boolean(diseasesQuery.data?.isOfflineFallback);

  return (
    <section
      id="almanac"
      className="w-full px-5 sm:px-8 lg:px-12 py-12"
      style={{
        backgroundColor: SEEDHOUSE_COLORS.parchment,
        color: SEEDHOUSE_COLORS.ink,
      }}
    >
      <div className="max-w-7xl mx-auto space-y-7">
        <div
          className="pb-5 border-b flex flex-wrap items-end justify-between gap-4"
          style={{ borderColor: SEEDHOUSE_COLORS.hairline }}
        >
          <div className="space-y-2 max-w-3xl">
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
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => {
                setSelectedPlantCode('');
                setExpandedDiseaseCode(null);
              }}
              aria-pressed={selectedPlantCode === ''}
              className="px-4 py-2.5 text-xs sm:text-sm font-bold border cursor-pointer transition-colors"
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
                  className="px-3.5 py-2.5 text-xs sm:text-sm font-bold border flex items-center gap-2 cursor-pointer transition-colors"
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
          className="border-t border-b divide-y"
          style={{ borderColor: SEEDHOUSE_COLORS.hairline }}
        >
          {diseasesQuery.isLoading && diseases.length === 0 ? (
            <div className="py-8 text-base" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
              ...
            </div>
          ) : diseases.length === 0 ? (
            <div className="py-8 text-base" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
              {selectedPlantCode && !keyword.trim() ? t.almanacHealthyOnlyCrop : t.almanacEmpty}
            </div>
          ) : (
            diseases.map((disease) => {
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
                    aria-expanded={isOpen}
                    onClick={() =>
                      setExpandedDiseaseCode((prev) =>
                        prev === disease.diseaseCode ? null : disease.diseaseCode
                      )
                    }
                    className="w-full px-4 sm:px-6 py-4 text-left flex flex-wrap items-center justify-between gap-4 cursor-pointer transition-colors hover:bg-[#FAF5EB]"
                    style={{
                      backgroundColor: isOpen ? SEEDHOUSE_COLORS.sheet : 'transparent',
                      color: SEEDHOUSE_COLORS.ink,
                    }}
                  >
                    <div className="space-y-1">
                      <div className="font-display text-lg sm:text-xl font-bold leading-snug">
                        {localizeDiseaseName(disease.diseaseCode, disease.commonName)}
                      </div>
                      {disease.scientificName && (
                        <div className="text-sm italic" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
                          {disease.scientificName}
                        </div>
                      )}
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
      </div>
    </section>
  );
}
