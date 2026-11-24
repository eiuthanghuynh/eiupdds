'use client';

import React, { useState } from 'react';
import { PddsApiError } from '@/lib/api/client';
import { SAMPLE_SPECIMENS } from '@/lib/data/sampleDiagnoses';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';
import { EyeReticleIcon, StampCheckIcon } from '@/components/primitives/WoodcutIcons';
import type { DiagnosisAnalyzeResponse } from '@/types/pdds';

export interface LeafSpecimenStageProps {
  diagnosis: DiagnosisAnalyzeResponse;
  previewUrl?: string | null;
  isSample: boolean;
  isLoading: boolean;
  error: PddsApiError | Error | null;
  onRetryUpload: () => void;
  onSelectSample: (sampleIndex: number) => void;
}

export function LeafSpecimenStage({
  diagnosis,
  previewUrl,
  isSample,
  isLoading,
  error,
  onRetryUpload,
  onSelectSample,
}: LeafSpecimenStageProps): React.JSX.Element {
  const { locale, t, localizeDiseaseName, localizePlantName, localizeSeverity } = useLanguage();
  const [showBoxes, setShowBoxes] = useState(true);

  const imgW = diagnosis.imageWidth && diagnosis.imageWidth > 0 ? diagnosis.imageWidth : 800;
  const imgH = diagnosis.imageHeight && diagnosis.imageHeight > 0 ? diagnosis.imageHeight : 520;
  const displaySrc = previewUrl || diagnosis.originalImageUrl;

  const diseaseCode = diagnosis.dominantDisease?.diseaseCode ?? 'LEAF_HEALTHY';
  const isHealthy = diseaseCode.endsWith('_HEALTHY');
  const diseaseTitle = diagnosis.dominantDisease
    ? localizeDiseaseName(diagnosis.dominantDisease.diseaseCode, diagnosis.dominantDisease.commonName)
    : locale === 'vi'
      ? 'Lá cây khỏe mạnh'
      : 'Healthy leaf';

  const plantTitle = diagnosis.plant
    ? localizePlantName(diagnosis.plant.plantCode, diagnosis.plant.commonName)
    : null;

  const confidencePct = Math.round((diagnosis.dominantConfidence ?? 0.88) * 100);
  const boxCount = diagnosis.boundingBoxes?.length ?? 0;
  const severity = isHealthy ? null : diagnosis.dominantDisease?.severity ?? 'MODERATE';
  const severityText = localizeSeverity(severity);

  const apiErr = error instanceof PddsApiError ? error : null;
  const is422 = apiErr?.status === 422 || apiErr?.code === 'DIAGNOSIS_003';
  const is429 = apiErr?.status === 429 || apiErr?.code === 'RATE_001';
  const retryMinutes = Math.max(1, Math.ceil((apiErr?.retryAfterSeconds ?? 1800) / 60));

  return (
    <div
      className="p-5 sm:p-7 flex flex-col justify-between gap-5"
      style={{ backgroundColor: SEEDHOUSE_COLORS.oliveStage, color: SEEDHOUSE_COLORS.creamWarm }}
    >
      {/* Top Specimen Header & Woodcut Stamp */}
      <div
        className="flex flex-wrap items-start justify-between gap-4 pb-4 border-b"
        style={{ borderColor: SEEDHOUSE_COLORS.oliveRule }}
      >
        <div className="space-y-1.5">
          <h2
            className="font-display text-xl sm:text-2xl font-bold leading-snug m-0"
            style={{ color: SEEDHOUSE_COLORS.creamWarm }}
          >
            {diseaseTitle}
          </h2>
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs sm:text-sm tabular-nums" style={{ color: '#E5DEC9' }}>
            {plantTitle && <span className="font-semibold">{plantTitle}</span>}
            {plantTitle && <span aria-hidden="true">·</span>}
            <span>{t.confidenceLabel(confidencePct, boxCount)}</span>
            {isSample && (
              <>
                <span aria-hidden="true">·</span>
                <span
                  className="underline decoration-dotted font-semibold"
                  style={{ color: SEEDHOUSE_COLORS.mustard }}
                >
                  {t.sampleReferenceBadge}
                </span>
              </>
            )}
          </div>
        </div>

        {/* Tactile Woodcut Verification Stamp */}
        <div
          key={diagnosis.diagnosisId}
          className="animate-ink-stamp px-3.5 py-2 border border-dashed flex items-center gap-2 text-xs font-bold tracking-[0.12em] uppercase shrink-0"
          style={
            isHealthy
              ? { borderColor: '#B8E0A8', color: '#E4F7DC', backgroundColor: '#2A3818' }
              : severity === 'CRITICAL' || severity === 'HIGH'
                ? { borderColor: SEEDHOUSE_COLORS.mustard, color: SEEDHOUSE_COLORS.mustard, backgroundColor: '#2B2114' }
                : { borderColor: '#E5DEC9', color: SEEDHOUSE_COLORS.creamWarm, backgroundColor: '#30381C' }
          }
        >
          <StampCheckIcon
            size={16}
            stroke={isHealthy ? '#E4F7DC' : SEEDHOUSE_COLORS.mustard}
          />
          <span>{severityText}</span>
        </div>
      </div>

      {/* Main Optical Mat: Loading / Error Recovery / Aspect-Ratio-Locked Leaf Canvas */}
      {isLoading ? (
        <div
          className="p-8 min-h-[260px] flex flex-col items-center justify-center text-center gap-3"
          style={{ backgroundColor: SEEDHOUSE_COLORS.oliveMat, color: SEEDHOUSE_COLORS.creamWarm }}
          role="status"
          aria-live="polite"
        >
          {previewUrl && (
            <img
              src={previewUrl}
              alt=""
              className="max-h-36 w-auto opacity-65 mb-2 object-contain"
            />
          )}
          <div className="font-display text-lg sm:text-xl font-bold">{t.loadingStageTitle}</div>
          <p className="text-sm max-w-md m-0" style={{ color: '#E2D8C1' }}>
            {t.loadingStageBody}
          </p>
        </div>
      ) : error ? (
        <div
          className="p-6 sm:p-8 min-h-[260px] flex flex-col justify-between gap-5"
          style={{ backgroundColor: SEEDHOUSE_COLORS.oliveMat, color: SEEDHOUSE_COLORS.creamWarm }}
          role="alert"
        >
          <div className="space-y-2.5">
            <h3 className="font-display text-lg sm:text-xl font-bold m-0" style={{ color: SEEDHOUSE_COLORS.mustard }}>
              {is422 ? t.error422Title : is429 ? t.error429Title : error.message || t.error422Title}
            </h3>
            <p className="text-sm sm:text-base leading-relaxed m-0" style={{ color: '#EDE4D1' }}>
              {is422
                ? t.error422Body
                : is429
                  ? t.error429Body(retryMinutes)
                  : apiErr?.status === 400
                    ? apiErr.message
                    : t.errorGenericBody}
            </p>
          </div>

          <div className="pt-4 border-t flex flex-wrap items-center justify-between gap-3" style={{ borderColor: '#4E5931' }}>
            {!is429 && (
              <button
                type="button"
                onClick={onRetryUpload}
                className="plaque-terracotta px-4 py-2.5 text-xs font-bold uppercase tracking-[0.12em] border-0 cursor-pointer"
              >
                {t.retryUploadButton}
              </button>
            )}
            <div className="flex flex-wrap items-center gap-2 text-xs">
              <span style={{ color: '#DED4BE' }}>{t.sampleGroupLabel}</span>
              {SAMPLE_SPECIMENS.map((item, idx) => (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => onSelectSample(idx)}
                  className="px-3 py-2 font-bold border cursor-pointer"
                  style={{
                    borderColor: SEEDHOUSE_COLORS.mustard,
                    backgroundColor: '#3B4423',
                    color: SEEDHOUSE_COLORS.creamWarm,
                  }}
                >
                  {locale === 'vi' ? item.shortLabelVi : item.shortLabelEn}
                </button>
              ))}
            </div>
          </div>
        </div>
      ) : (
        <div
          className="p-3 sm:p-4 flex items-center justify-center"
          style={{ backgroundColor: SEEDHOUSE_COLORS.oliveMat }}
        >
          <div
            data-testid="specimen-aspect-frame"
            className="relative w-full mx-auto overflow-hidden"
            style={{
              aspectRatio: `${imgW} / ${imgH}`,
              maxHeight: '380px',
              maxWidth: `${Math.round((380 * imgW) / imgH)}px`,
            }}
          >
            <img
              src={displaySrc}
              alt={diseaseTitle}
              className="block w-full h-full object-fill select-none"
            />
            {showBoxes && boxCount > 0 && (
              <div
                data-testid="bbox-overlay-group"
                className="absolute inset-0 w-full h-full pointer-events-none"
              >
                <svg
                  viewBox="0 0 1000 1000"
                  preserveAspectRatio="none"
                  className="absolute inset-0 w-full h-full"
                >
                  {diagnosis.boundingBoxes.map((box) => {
                    const x = Math.max(0, Math.min(980, box.xMinNormalized * 1000));
                    const y = Math.max(0, Math.min(980, box.yMinNormalized * 1000));
                    const w = Math.max(20, Math.min(1000 - x, box.widthNormalized * 1000));
                    const h = Math.max(20, Math.min(1000 - y, box.heightNormalized * 1000));

                    return (
                      <rect
                        key={box.id}
                        x={x}
                        y={y}
                        width={w}
                        height={h}
                        fill="rgba(245, 207, 122, 0.12)"
                        stroke={SEEDHOUSE_COLORS.mustard}
                        strokeWidth="2.5"
                        strokeDasharray="8 4"
                        vectorEffect="non-scaling-stroke"
                      />
                    );
                  })}
                </svg>

                {diagnosis.boundingBoxes.map((box) => {
                  const anchorRight = box.xMinNormalized > 0.45;
                  const rightPct = Math.round(
                    Math.max(2, Math.min(42, (1 - (box.xMinNormalized + box.widthNormalized)) * 100))
                  );
                  const leftPct = Math.round(Math.max(2, Math.min(42, box.xMinNormalized * 100)));
                  const maxWidthPct = anchorRight ? 98 - rightPct : 98 - leftPct;
                  const topRaw = box.yMinNormalized * 100;
                  const placeAbove = topRaw >= 13;
                  const topPct = placeAbove
                    ? Math.max(1.5, topRaw - 10)
                    : Math.min(85, (box.yMinNormalized + box.heightNormalized) * 100 + 2);
                  const boxPct = Math.round((box.confidence ?? 0.85) * 100);
                  const rawTitle = box.diseaseCode
                    ? localizeDiseaseName(box.diseaseCode, box.label)
                    : box.label;
                  const compactTitle = rawTitle.replace(/\s*\([^)]*\)/g, '').trim();

                  return (
                    <span
                      key={`${box.id}-tag`}
                      data-testid="bbox-label-pill"
                      className="absolute px-2 py-0.5 text-xs font-bold leading-snug inline-flex items-center border tabular-nums overflow-hidden"
                      style={{
                        ...(anchorRight ? { right: `${rightPct}%` } : { left: `${leftPct}%` }),
                        top: `${topPct}%`,
                        maxWidth: `${maxWidthPct}%`,
                        backgroundColor: '#241C14',
                        borderColor: SEEDHOUSE_COLORS.mustard,
                        color: SEEDHOUSE_COLORS.creamWarm,
                      }}
                    >
                      <span className="truncate">{compactTitle}</span>
                      <span className="shrink-0 ml-1">· {boxPct}%</span>
                    </span>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Bottom Stage Controls: Toggle Lesion Reticles */}
      <div
        className="pt-3 border-t flex flex-wrap items-center justify-between gap-3 text-xs"
        style={{ borderColor: SEEDHOUSE_COLORS.oliveRule }}
      >
        <span style={{ color: '#E2D8C1' }}>
          {diagnosis.dominantDisease?.scientificName ? (
            <em>{diagnosis.dominantDisease.scientificName}</em>
          ) : (
            <span>EIU PDDS Computer Vision</span>
          )}
        </span>

        {boxCount > 0 && !error && !isLoading && (
          <button
            type="button"
            onClick={() => setShowBoxes((prev) => !prev)}
            aria-pressed={showBoxes}
            className="px-3.5 py-2 font-bold uppercase tracking-[0.1em] border flex items-center gap-2 cursor-pointer transition-colors"
            style={{
              borderColor: SEEDHOUSE_COLORS.mustard,
              backgroundColor: '#2B3318',
              color: SEEDHOUSE_COLORS.creamWarm,
            }}
          >
            <EyeReticleIcon size={16} stroke={SEEDHOUSE_COLORS.mustard} />
            <span>{showBoxes ? t.toggleBoxesHide : t.toggleBoxesShow}</span>
          </button>
        )}
      </div>
    </div>
  );
}
