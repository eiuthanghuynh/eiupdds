'use client';

import React, { useState } from 'react';
import { PddsApiError } from '@/lib/api/client';
import { SAMPLE_SPECIMENS } from '@/lib/data/sampleDiagnoses';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';
import {
  EyeReticleIcon,
  StampAlertIcon,
  StampCheckIcon,
} from '@/components/primitives/WoodcutIcons';
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
  const { t, localizeDiseaseName, localizePlantName, localizeSeverity, localizeSampleLabel } =
    useLanguage();
  const [showBoxes, setShowBoxes] = useState(true);
  const [naturalDims, setNaturalDims] = useState<{ w: number; h: number } | null>(null);

  const imgW =
    diagnosis.imageWidth && diagnosis.imageWidth > 0
      ? diagnosis.imageWidth
      : naturalDims?.w ?? 800;
  const imgH =
    diagnosis.imageHeight && diagnosis.imageHeight > 0
      ? diagnosis.imageHeight
      : naturalDims?.h ?? 520;
  const displaySrc = previewUrl || diagnosis.originalImageUrl;

  const diseaseCode = diagnosis.dominantDisease?.diseaseCode ?? 'LEAF_HEALTHY';
  const isHealthy = diseaseCode.endsWith('_HEALTHY');
  const diseaseTitle = diagnosis.dominantDisease
    ? localizeDiseaseName(diagnosis.dominantDisease.diseaseCode, diagnosis.dominantDisease.commonName)
    : t.healthyLeafTitle;

  const plantTitle = diagnosis.plant
    ? localizePlantName(diagnosis.plant.plantCode, diagnosis.plant.commonName)
    : null;

  const confidencePct = Math.round((diagnosis.dominantConfidence ?? 0.88) * 100);
  const boxCount = diagnosis.boundingBoxes?.length ?? 0;
  const severity = isHealthy ? null : diagnosis.dominantDisease?.severity ?? 'MODERATE';
  const severityText = localizeSeverity(severity);
  const isSevere = !isHealthy && (severity === 'CRITICAL' || severity === 'HIGH');

  const apiErr = error instanceof PddsApiError ? error : null;
  const is400 = apiErr?.status === 400;
  const is422 = apiErr?.status === 422 || apiErr?.code === 'DIAGNOSIS_003';
  const is429 = apiErr?.status === 429 || apiErr?.code === 'RATE_001';
  const retryMinutes = Math.max(1, Math.ceil((apiErr?.retryAfterSeconds ?? 1800) / 60));
  const showActiveResult = !isLoading && !error;

  const placedPillBands: Array<{ x1: number; x2: number; y1: number; y2: number }> = [];
  const pillLayouts = (diagnosis.boundingBoxes ?? []).map((box, idx, allBoxes) => {
    const anchorRight = box.xMinNormalized > 0.45;
    const rightPct = Math.round(
      Math.max(2, Math.min(42, (1 - (box.xMinNormalized + box.widthNormalized)) * 100))
    );
    const leftPct = Math.round(Math.max(2, Math.min(42, box.xMinNormalized * 100)));
    const maxWidthPct = anchorRight ? 98 - rightPct : 98 - leftPct;

    // Reserve conservative 72% normalized pill span width so long localized labels never collide on 390px mobile
    const pillSpanW = 0.72;
    const pillBandH = 0.145;
    const pillX1 = anchorRight ? Math.max(0, 1 - rightPct / 100 - pillSpanW) : leftPct / 100;
    const pillX2 = anchorRight ? 1 - rightPct / 100 : Math.min(1, leftPct / 100 + pillSpanW);

    const overlapsBox = (y1: number, y2: number) =>
      allBoxes.some((other, otherIdx) => {
        if (otherIdx === idx) return false;
        const ox1 = other.xMinNormalized - 0.02;
        const ox2 = other.xMinNormalized + other.widthNormalized + 0.02;
        const oy1 = other.yMinNormalized - 0.02;
        const oy2 = other.yMinNormalized + other.heightNormalized + 0.02;
        return pillX1 <= ox2 && pillX2 >= ox1 && y1 <= oy2 && y2 >= oy1;
      });

    const overlapsPill = (y1: number, y2: number) =>
      placedPillBands.some(
        (b) => pillX1 <= b.x2 && pillX2 >= b.x1 && y1 < b.y2 && y2 > b.y1
      );

    const aboveY1 = box.yMinNormalized - pillBandH;
    const aboveY2 = box.yMinNormalized;
    const belowY1 = box.yMinNormalized + box.heightNormalized;
    const belowY2 = belowY1 + pillBandH;

    const canPlaceAbove =
      box.yMinNormalized >= 0.14 && !overlapsBox(aboveY1, aboveY2) && !overlapsPill(aboveY1, aboveY2);
    const canPlaceBelow =
      belowY2 <= 0.99 && !overlapsBox(belowY1, belowY2) && !overlapsPill(belowY1, belowY2);

    let placement: 'above' | 'below' | 'inside' = 'inside';
    let topPct = Math.min(86, box.yMinNormalized * 100 + 1.5);
    let bandY1 = box.yMinNormalized + 0.015;
    let bandY2 = bandY1 + pillBandH;

    if (canPlaceAbove) {
      placement = 'above';
      topPct = box.yMinNormalized * 100;
      bandY1 = aboveY1;
      bandY2 = aboveY2;
    } else if (canPlaceBelow) {
      placement = 'below';
      topPct = belowY1 * 100;
      bandY1 = belowY1;
      bandY2 = belowY2;
    } else {
      const candidateOffsets = [0.015, 0.17, 0.33, -0.11, 0.49];
      for (const offset of candidateOffsets) {
        const cy1 = Math.max(0.02, Math.min(0.84, box.yMinNormalized + offset));
        const cy2 = cy1 + pillBandH;
        if (!overlapsPill(cy1, cy2)) {
          topPct = cy1 * 100;
          bandY1 = cy1;
          bandY2 = cy2;
          break;
        }
      }
    }

    placedPillBands.push({ x1: pillX1, x2: pillX2, y1: bandY1, y2: bandY2 });

    return {
      box,
      anchorRight,
      rightPct,
      leftPct,
      maxWidthPct,
      placement,
      topPct,
    };
  });

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
            {isLoading
              ? t.stageNeutralLoadingHeading
              : error
                ? t.stageNeutralErrorHeading
                : diseaseTitle}
          </h2>
          {showActiveResult && (
            <div
              className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs sm:text-sm tabular-nums"
              style={{ color: '#E5DEC9' }}
            >
              {plantTitle && <span className="font-semibold">{plantTitle}</span>}
              {plantTitle && <span aria-hidden="true">·</span>}
              <span>{t.confidenceLabel(confidencePct, boxCount)}</span>
              {isSample && (
                <>
                  <span aria-hidden="true">·</span>
                  <span
                    className="font-semibold"
                    style={{ color: SEEDHOUSE_COLORS.mustard }}
                  >
                    {t.sampleReferenceBadge}
                  </span>
                </>
              )}
            </div>
          )}
        </div>

        {/* Tactile Woodcut Verification Stamp (only when valid result is shown) */}
        {showActiveResult && (
          <div
            key={diagnosis.diagnosisId}
            className="animate-ink-stamp px-3.5 py-2 border border-dashed flex items-center gap-2 text-xs font-bold tracking-[0.12em] uppercase shrink-0"
            style={
              isHealthy
                ? { borderColor: '#B8E0A8', color: '#E4F7DC', backgroundColor: '#2A3818' }
                : severity === 'CRITICAL'
                  ? {
                      borderColor: SEEDHOUSE_COLORS.mustard,
                      color: '#FFFDF9',
                      backgroundColor: SEEDHOUSE_COLORS.critical,
                    }
                  : severity === 'HIGH'
                    ? {
                        borderColor: SEEDHOUSE_COLORS.mustard,
                        color: SEEDHOUSE_COLORS.mustard,
                        backgroundColor: '#5C2112',
                      }
                    : {
                        borderColor: '#E5DEC9',
                        color: SEEDHOUSE_COLORS.creamWarm,
                        backgroundColor: '#30381C',
                      }
            }
          >
            {isSevere ? (
              <StampAlertIcon
                size={16}
                stroke={severity === 'CRITICAL' ? '#FFFDF9' : SEEDHOUSE_COLORS.mustard}
                testId="severity-alert-icon"
              />
            ) : (
              <StampCheckIcon
                size={16}
                stroke={isHealthy ? '#E4F7DC' : SEEDHOUSE_COLORS.creamWarm}
                testId="severity-check-icon"
              />
            )}
            <span>{severityText}</span>
          </div>
        )}
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
          <div className="space-y-3">
            {is422 && previewUrl && (
              <img
                data-testid="error-422-preview"
                src={previewUrl}
                alt=""
                className="max-h-28 w-auto object-contain border"
                style={{ borderColor: SEEDHOUSE_COLORS.oliveRule }}
              />
            )}
            <h3
              className="font-display text-lg sm:text-xl font-bold m-0"
              style={{ color: SEEDHOUSE_COLORS.mustard }}
            >
              {is422
                ? t.error422Title
                : is429
                  ? t.error429Title
                  : is400
                    ? t.error400Title
                    : error.message || t.error422Title}
            </h3>
            <p className="text-sm sm:text-base leading-relaxed m-0" style={{ color: '#EDE4D1' }}>
              {is422
                ? t.error422Body
                : is429
                  ? t.error429Body(retryMinutes)
                  : is400 && apiErr
                    ? apiErr.code === 'INVALID_FILE_TYPE'
                      ? t.errorInvalidFileType
                      : apiErr.code === 'FILE_TOO_LARGE'
                        ? t.errorFileTooLarge
                        : apiErr.message
                    : t.errorGenericBody}
            </p>
          </div>

          <div className="pt-4 border-t flex flex-wrap items-center justify-between gap-3" style={{ borderColor: '#4E5931' }}>
            {!is429 && (
              <button
                type="button"
                onClick={onRetryUpload}
                className="plaque-terracotta min-h-[44px] px-4 py-2.5 text-xs font-bold uppercase tracking-[0.12em] border-0 cursor-pointer"
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
                  className="min-h-[44px] px-3 py-2 font-bold border cursor-pointer"
                  style={{
                    borderColor: SEEDHOUSE_COLORS.mustard,
                    backgroundColor: '#3B4423',
                    color: SEEDHOUSE_COLORS.creamWarm,
                  }}
                >
                  {localizeSampleLabel(item)}
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
              maxHeight: '350px',
              maxWidth: `${Math.round((350 * imgW) / imgH)}px`,
            }}
          >
            <img
              src={displaySrc}
              alt={diseaseTitle}
              onLoad={(e) => {
                const img = e.currentTarget;
                if (img.naturalWidth > 0 && img.naturalHeight > 0) {
                  setNaturalDims({ w: img.naturalWidth, h: img.naturalHeight });
                }
              }}
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

                {pillLayouts.map(
                  ({ box, anchorRight, rightPct, leftPct, maxWidthPct, placement, topPct }) => {
                    const boxPct = Math.round((box.confidence ?? 0.85) * 100);
                    const rawTitle = box.diseaseCode
                      ? localizeDiseaseName(box.diseaseCode, box.label)
                      : box.label;
                    const compactTitle = rawTitle.replace(/\s*\([^)]*\)/g, '').trim();
                    const pillLabel = compactTitle;

                    return (
                      <span
                        key={`${box.id}-tag`}
                        data-testid="bbox-label-pill"
                        data-placement={placement}
                        className="absolute px-2 py-0.5 text-xs font-bold leading-snug inline-flex items-center border tabular-nums overflow-hidden"
                        style={{
                          ...(anchorRight ? { right: `${rightPct}%` } : { left: `${leftPct}%` }),
                          top: `${topPct}%`,
                          transform:
                            placement === 'above'
                              ? 'translateY(calc(-100% - 4px))'
                              : 'translateY(4px)',
                          maxWidth: `${maxWidthPct}%`,
                          backgroundColor: '#241C14',
                          borderColor: SEEDHOUSE_COLORS.mustard,
                          color: SEEDHOUSE_COLORS.creamWarm,
                        }}
                      >
                        <span className="truncate">{pillLabel}</span>
                        <span className="shrink-0">&nbsp;· {boxPct}%</span>
                      </span>
                    );
                  }
                )}
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
        <span style={{ color: '#E2D8C1' }}>EIU PDDS</span>

        {boxCount > 0 && !error && !isLoading && (
          <button
            type="button"
            onClick={() => setShowBoxes((prev) => !prev)}
            aria-pressed={showBoxes}
            className="min-h-[44px] px-3.5 py-2 font-bold uppercase tracking-[0.1em] border flex items-center gap-2 cursor-pointer transition-colors"
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
