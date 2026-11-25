'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { PddsApiError, validateLeafFile } from '@/lib/api/client';
import { useAnalyzeLeafMutation, usePlantsQuery } from '@/lib/api/queries';
import { SAMPLE_SPECIMENS } from '@/lib/data/sampleDiagnoses';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';
import { UploadArrowIcon } from '@/components/primitives/WoodcutIcons';
import { LeafSpecimenStage } from '@/components/diagnosis/LeafSpecimenStage';
import { PrescriptionSpread } from '@/components/diagnosis/PrescriptionSpread';
import type { DiagnosisAnalyzeResponse, GuestQuotaInfo } from '@/types/pdds';

function measureLocalImageDimensions(url: string): Promise<{ width: number; height: number }> {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => {
      resolve({
        width: img.naturalWidth || 800,
        height: img.naturalHeight || 600,
      });
    };
    img.onerror = () => resolve({ width: 800, height: 600 });
    img.src = url;
  });
}

export function DiagnosticWorkbench(): React.JSX.Element {
  const { t, localizePlantName, localizeSampleLabel, localizeSampleResponse } = useLanguage();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const stageContainerRef = useRef<HTMLDivElement | null>(null);

  const [selectedSampleIndex, setSelectedSampleIndex] = useState<number | null>(0);
  const [liveDiagnosis, setLiveDiagnosis] = useState<DiagnosisAnalyzeResponse | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [plantCodeHint, setPlantCodeHint] = useState<string>('');
  const [clientError, setClientError] = useState<PddsApiError | Error | null>(null);
  const [quota, setQuota] = useState<GuestQuotaInfo>({ limit: 3, remaining: 3 });
  const [isDragOver, setIsDragOver] = useState(false);

  const plantsQuery = usePlantsQuery();
  const analyzeMutation = useAnalyzeLeafMutation();

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const scrollToStageOnCompactScreen = useCallback(() => {
    if (typeof window !== 'undefined' && window.innerWidth < 1024) {
      stageContainerRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, []);

  const activeDiagnosis: DiagnosisAnalyzeResponse =
    selectedSampleIndex !== null
      ? localizeSampleResponse(SAMPLE_SPECIMENS[selectedSampleIndex])
      : liveDiagnosis ?? localizeSampleResponse(SAMPLE_SPECIMENS[0]);

  const activeError = clientError ?? (analyzeMutation.error as PddsApiError | Error | null);

  const handleSelectSample = (index: number) => {
    setClientError(null);
    analyzeMutation.reset();
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl(null);
    }
    setSelectedSampleIndex(index);
    scrollToStageOnCompactScreen();
  };

  const handleFileUpload = useCallback(
    async (file: File) => {
      setClientError(null);
      try {
        validateLeafFile(file);
      } catch (err) {
        setSelectedSampleIndex(null);
        setClientError(err as PddsApiError);
        scrollToStageOnCompactScreen();
        return;
      }

      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      const nextUrl = URL.createObjectURL(file);
      setPreviewUrl(nextUrl);
      setSelectedSampleIndex(null);
      scrollToStageOnCompactScreen();

      try {
        const dims = await measureLocalImageDimensions(nextUrl);
        const { result, quota: nextQuota } = await analyzeMutation.mutateAsync({
          file,
          plantCode: plantCodeHint || undefined,
        });
        setLiveDiagnosis({
          ...result,
          imageWidth: result.imageWidth || dims.width,
          imageHeight: result.imageHeight || dims.height,
        });
        setQuota(nextQuota);
      } catch (err) {
        if (err instanceof PddsApiError) {
          if (err.quota) {
            setQuota(err.quota);
          } else if (err.status === 429) {
            setQuota((prev) => ({
              ...prev,
              remaining: 0,
              retryAfterSeconds: err.retryAfterSeconds,
            }));
          }
        }
        setClientError(err as Error);
      }
    },
    [analyzeMutation, plantCodeHint, previewUrl, scrollToStageOnCompactScreen]
  );

  useEffect(() => {
    const handlePaste = (e: ClipboardEvent) => {
      if (analyzeMutation.isPending) return;
      const items = e.clipboardData?.items;
      if (!items) return;
      for (const item of Array.from(items)) {
        if (item.type.startsWith('image/')) {
          const pasted = item.getAsFile();
          if (pasted) {
            void handleFileUpload(pasted);
            break;
          }
        }
      }
    };
    window.addEventListener('paste', handlePaste);
    return () => window.removeEventListener('paste', handlePaste);
  }, [analyzeMutation.isPending, handleFileUpload]);

  const openFilePicker = () => {
    fileInputRef.current?.click();
  };

  const cropOptions = [
    { value: '', label: t.cropHintAll },
    ...(plantsQuery.data?.plants ?? []).map((p) => ({
      value: p.plantCode,
      label: localizePlantName(p.plantCode, p.commonName),
    })),
  ];

  return (
    <div id="workbench" className="w-full">
      {/* First Viewport: 5:7 Kraft Controls & Recessed Olive Specimen Stage */}
      <section className="w-full bg-kraft-ribbed px-4 sm:px-8 lg:px-12 pt-5 pb-7 sm:pt-6 sm:pb-8">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-10 items-stretch">
          {/* Left 5 Cols: Distilled Headline, Primary Upload CTA, Native Crop Hint & 3 Sample Triggers */}
          <div className="lg:col-span-5 flex flex-col justify-between gap-5 py-1">
            <div className="space-y-3">
              <h1
                className="font-display text-2xl sm:text-4xl font-bold uppercase tracking-[0.04em] leading-[1.15] m-0"
                style={{ color: SEEDHOUSE_COLORS.ink }}
              >
                {t.heroHeading}
              </h1>
              <hr className="border-0 border-t m-0" style={{ borderColor: '#9B8B70' }} />
              <p
                className="text-base leading-relaxed m-0 max-w-[54ch]"
                style={{ color: SEEDHOUSE_COLORS.inkMuted }}
              >
                {t.heroLead}
              </p>
            </div>

            <div className="space-y-4">
              {/* Hidden Native File/Camera Input + Stitched Terracotta CTA */}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="sr-only"
                onChange={(e) => {
                  const picked = e.target.files?.[0];
                  if (picked) {
                    void handleFileUpload(picked);
                  }
                  e.target.value = '';
                }}
              />

              <div
                onDragOver={(e) => {
                  e.preventDefault();
                  if (!analyzeMutation.isPending) {
                    setIsDragOver(true);
                  }
                }}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={(e) => {
                  e.preventDefault();
                  setIsDragOver(false);
                  if (analyzeMutation.isPending) return;
                  const dropped = e.dataTransfer.files?.[0];
                  if (dropped) {
                    void handleFileUpload(dropped);
                  }
                }}
                className="p-3 sm:p-4 border border-dashed space-y-2.5 transition-colors"
                style={{
                  borderColor: isDragOver ? SEEDHOUSE_COLORS.terracotta : '#8E7D63',
                  backgroundColor: isDragOver ? '#FAF5EB' : '#EFE6D4',
                }}
              >
                <button
                  type="button"
                  onClick={openFilePicker}
                  disabled={analyzeMutation.isPending}
                  className="plaque-terracotta min-h-[48px] w-full py-3.5 px-5 text-sm font-bold uppercase tracking-[0.14em] border-0 flex items-center justify-center gap-3 cursor-pointer disabled:opacity-60"
                >
                  <UploadArrowIcon size={18} stroke="#FFFDF9" />
                  <span>{t.uploadCta}</span>
                </button>
                <p className="text-xs text-center m-0" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
                  {t.uploadPhotoTip}
                </p>
              </div>

              <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-semibold tabular-nums">
                <span style={{ color: SEEDHOUSE_COLORS.olive }}>
                  {t.quotaLabel(quota.remaining, quota.limit)}
                </span>
                {selectedSampleIndex === null && (
                  <button
                    type="button"
                    onClick={() => handleSelectSample(0)}
                    className="min-h-[44px] px-2.5 text-xs font-bold underline cursor-pointer bg-transparent border-0"
                    style={{ color: SEEDHOUSE_COLORS.terracotta }}
                  >
                    {t.resetUploadButton}
                  </button>
                )}
              </div>

              {/* Optional Crop Hint (Native zero-FOUC select placed below primary CTA) */}
              <div className="space-y-1.5">
                <label
                  htmlFor="crop-hint-select"
                  className="block text-xs font-bold uppercase tracking-[0.06em]"
                  style={{ color: SEEDHOUSE_COLORS.inkMuted }}
                >
                  {t.cropHintLabel}
                </label>
                <select
                  id="crop-hint-select"
                  value={plantCodeHint}
                  onChange={(e) => setPlantCodeHint(e.target.value)}
                  aria-label={t.cropHintLabel}
                  className="w-full min-h-[44px] px-3.5 text-sm font-medium border rounded-[2px] cursor-pointer"
                  style={{
                    backgroundColor: SEEDHOUSE_COLORS.sheet,
                    borderColor: SEEDHOUSE_COLORS.hairline,
                    color: SEEDHOUSE_COLORS.ink,
                  }}
                >
                  {cropOptions.map((opt) => (
                    <option key={opt.value || 'AUTO'} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* 3 Sample Reference Specimens */}
              <div
                className="pt-3 border-t space-y-2"
                style={{ borderColor: '#B5A488' }}
              >
                <div
                  className="text-xs font-bold uppercase tracking-[0.1em]"
                  style={{ color: SEEDHOUSE_COLORS.inkMuted }}
                >
                  {t.sampleGroupLabel}
                </div>
                <div className="flex flex-wrap gap-2">
                  {SAMPLE_SPECIMENS.map((item, idx) => {
                    const isActive = selectedSampleIndex === idx && !activeError;
                    return (
                      <button
                        key={item.key}
                        type="button"
                        onClick={() => handleSelectSample(idx)}
                        className="min-h-[44px] px-3.5 py-2 text-xs sm:text-sm font-bold border cursor-pointer transition-colors"
                        style={
                          isActive
                            ? {
                                backgroundColor: SEEDHOUSE_COLORS.olive,
                                borderColor: SEEDHOUSE_COLORS.olive,
                                color: '#FAF3E6',
                              }
                            : {
                                backgroundColor: SEEDHOUSE_COLORS.sheet,
                                borderColor: '#B5A488',
                                color: SEEDHOUSE_COLORS.ink,
                              }
                        }
                      >
                        {localizeSampleLabel(item)}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>

          {/* Right 7 Cols: Recessed Olive Specimen Stage */}
          <div ref={stageContainerRef} className="lg:col-span-7 flex flex-col">
            <LeafSpecimenStage
              diagnosis={activeDiagnosis}
              previewUrl={selectedSampleIndex === null ? previewUrl : null}
              isSample={selectedSampleIndex !== null}
              isLoading={analyzeMutation.isPending}
              error={activeError}
              onRetryUpload={openFilePicker}
              onSelectSample={handleSelectSample}
            />
          </div>
        </div>
      </section>

      {/* Full-Width Unboxed 3-Column Hairline Prescription Spread (only when valid result is active) */}
      {!activeError && !analyzeMutation.isPending && (
        <PrescriptionSpread diagnosis={activeDiagnosis} />
      )}
    </div>
  );
}
