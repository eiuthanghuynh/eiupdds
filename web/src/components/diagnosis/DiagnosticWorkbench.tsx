'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Select } from 'antd';
import { PddsApiError, validateLeafFile } from '@/lib/api/client';
import { useAnalyzeLeafMutation, usePlantsQuery } from '@/lib/api/queries';
import { getLocalizedSampleResponse, SAMPLE_SPECIMENS } from '@/lib/data/sampleDiagnoses';
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
  const { locale, t, localizePlantName } = useLanguage();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

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

  const activeDiagnosis: DiagnosisAnalyzeResponse =
    selectedSampleIndex !== null
      ? getLocalizedSampleResponse(SAMPLE_SPECIMENS[selectedSampleIndex], locale)
      : liveDiagnosis ?? getLocalizedSampleResponse(SAMPLE_SPECIMENS[0], locale);

  const activeError = clientError ?? (analyzeMutation.error as PddsApiError | Error | null);

  const handleSelectSample = (index: number) => {
    setClientError(null);
    analyzeMutation.reset();
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl(null);
    }
    setSelectedSampleIndex(index);
  };

  const handleFileUpload = async (file: File) => {
    setClientError(null);
    try {
      validateLeafFile(file);
    } catch (err) {
      setSelectedSampleIndex(null);
      setClientError(err as PddsApiError);
      return;
    }

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    const nextUrl = URL.createObjectURL(file);
    setPreviewUrl(nextUrl);
    setSelectedSampleIndex(null);

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
  };

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
    <div id="ban-kham" className="w-full">
      {/* First Viewport: 5:7 Ribbed Kraft Hero & Olive Specimen Stage */}
      <section className="w-full bg-kraft-ribbed px-5 sm:px-8 lg:px-12 pt-8 pb-10">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-10 items-stretch">
          {/* Left 5 Cols: Farmer Greeting, Crop Hint, Upload Plaque & 3 Sample Triggers */}
          <div className="lg:col-span-5 flex flex-col justify-between gap-6 py-1">
            <div className="space-y-4">
              <h1
                className="font-display text-3xl sm:text-4xl xl:text-[2.65rem] font-bold uppercase tracking-[0.04em] leading-[1.15] m-0"
                style={{ color: SEEDHOUSE_COLORS.ink }}
              >
                {t.heroHeading}
              </h1>
              <hr
                className="border-0 border-t m-0"
                style={{ borderColor: '#9B8B70' }}
              />
              <p
                className="text-base sm:text-[17px] leading-relaxed m-0 max-w-[62ch]"
                style={{ color: SEEDHOUSE_COLORS.inkMuted }}
              >
                {t.heroLead}
              </p>
            </div>

            <div className="space-y-5">
              {/* Optional Crop Hint */}
              <div className="space-y-2">
                <label
                  htmlFor="crop-hint-select"
                  className="block text-sm font-bold"
                  style={{ color: SEEDHOUSE_COLORS.ink }}
                >
                  {t.cropHintLabel}
                </label>
                <Select
                  id="crop-hint-select"
                  size="large"
                  value={plantCodeHint}
                  onChange={(value) => setPlantCodeHint(value)}
                  options={cropOptions}
                  className="w-full"
                  aria-label={t.cropHintLabel}
                />
              </div>

              {/* Hidden Native File/Camera Input + Tactile Dropzone & Terracotta Stitched CTA */}
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
                className="p-4 border border-dashed space-y-3 transition-colors"
                style={{
                  borderColor: isDragOver ? SEEDHOUSE_COLORS.terracotta : '#8E7D63',
                  backgroundColor: isDragOver ? '#FAF5EB' : '#EFE6D4',
                }}
              >
                <button
                  type="button"
                  onClick={openFilePicker}
                  disabled={analyzeMutation.isPending}
                  className="plaque-terracotta w-full py-4 px-6 text-sm font-bold uppercase tracking-[0.14em] border-0 flex items-center justify-center gap-3 cursor-pointer disabled:opacity-60"
                >
                  <UploadArrowIcon size={18} stroke="#FFFDF9" />
                  <span>{t.uploadCta}</span>
                </button>
                <p className="text-xs text-center m-0" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
                  {t.uploadDropHint}
                </p>
              </div>

              <div className="text-xs font-semibold tabular-nums" style={{ color: SEEDHOUSE_COLORS.olive }}>
                {t.quotaLabel(quota.remaining, quota.limit)}
              </div>

              {/* 3 Sample Reference Specimens */}
              <div
                className="pt-4 border-t space-y-2.5"
                style={{ borderColor: '#B5A488' }}
              >
                <div className="text-xs font-bold uppercase tracking-[0.1em]" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
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
                        className="px-3.5 py-2 text-xs sm:text-sm font-bold border cursor-pointer transition-colors"
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
                        {locale === 'vi' ? item.shortLabelVi : item.shortLabelEn}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>

          {/* Right 7 Cols: Recessed Olive Specimen Stage */}
          <div className="lg:col-span-7 flex flex-col">
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
