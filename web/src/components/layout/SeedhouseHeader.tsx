'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { QRCode } from 'antd';
import { AnimatePresence, m } from 'motion/react';
import { DeckledEdge } from '@/components/primitives/DeckledEdge';
import { EiuUniversityMark } from '@/components/primitives/EiuUniversityMark';
import { StampCheckIcon } from '@/components/primitives/WoodcutIcons';
import { OPEN_APP_SHEET_EVENT, getMobileAppDownloadUrl } from '@/lib/config/mobileApp';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';

export function SeedhouseHeader(): React.JSX.Element {
  const { locale, setLocale, t } = useLanguage();
  const [appSheetOpen, setAppSheetOpen] = useState(false);
  const [openRequestSeq, setOpenRequestSeq] = useState(0);
  const pendingScrollTopRef = useRef(false);
  const closeButtonRef = useRef<HTMLButtonElement | null>(null);

  const downloadUrl = getMobileAppDownloadUrl();
  const nextLocale = locale === 'vi' ? 'en' : 'vi';
  const toggleAria = locale === 'vi' ? t.langToggleAriaToEn : t.langToggleAriaToVi;

  const scrollToHeaderTop = useCallback(() => {
    if (typeof window.scrollTo === 'function') {
      const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
      window.scrollTo({ top: 0, behavior: reduced ? 'auto' : 'smooth' });
    }
  }, []);

  useEffect(() => {
    const handleOpenDrawer = () => {
      if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur();
      }
      pendingScrollTopRef.current = true;
      setAppSheetOpen(true);
      setOpenRequestSeq((seq) => seq + 1);
    };
    window.addEventListener(OPEN_APP_SHEET_EVENT, handleOpenDrawer);
    return () => window.removeEventListener(OPEN_APP_SHEET_EVENT, handleOpenDrawer);
  }, []);

  useEffect(() => {
    if (openRequestSeq === 0) return;
    let raf2 = 0;
    const raf1 = window.requestAnimationFrame(() => {
      raf2 = window.requestAnimationFrame(() => {
        scrollToHeaderTop();
      });
    });
    const fallbackTimer = window.setTimeout(() => {
      scrollToHeaderTop();
    }, 80);
    return () => {
      window.cancelAnimationFrame(raf1);
      window.cancelAnimationFrame(raf2);
      window.clearTimeout(fallbackTimer);
    };
  }, [openRequestSeq, scrollToHeaderTop]);

  useEffect(() => {
    if (!appSheetOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setAppSheetOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [appSheetOpen]);

  return (
    <header className="w-full">
      <div
        className="w-full px-3.5 sm:px-8 lg:px-12 py-1 sm:py-3"
        style={{ backgroundColor: SEEDHOUSE_COLORS.olive, color: SEEDHOUSE_COLORS.cream }}
      >
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-x-2 sm:gap-x-4 gap-y-0">
          <a
            href="#workbench"
            className="flex items-center gap-2 sm:gap-3 no-underline group min-h-[44px] shrink-0"
            style={{ color: SEEDHOUSE_COLORS.cream }}
          >
            <EiuUniversityMark size={34} />
            <div>
              <div className="font-display text-sm sm:text-xl font-bold tracking-[0.12em] sm:tracking-[0.2em] uppercase leading-tight whitespace-nowrap">
                EIU PDDS
              </div>
              <div
                className="hidden sm:block text-xs tracking-[0.08em] uppercase"
                style={{ color: '#E2D6BE' }}
              >
                {t.brandSubtitle}
              </div>
            </div>
          </a>

          <nav
            aria-label={t.navAriaLabel}
            className="order-3 lg:order-none w-full lg:w-auto pt-0 border-t lg:border-t-0 flex items-center gap-x-5 text-xs font-semibold tracking-[0.1em] uppercase"
            style={{ borderColor: '#697349' }}
          >
            <a
              href="#workbench"
              className="min-h-[44px] inline-flex items-center hover:underline"
              style={{ color: SEEDHOUSE_COLORS.cream }}
            >
              {t.navWorkbench}
            </a>
            <a
              href="#almanac"
              className="min-h-[44px] inline-flex items-center hover:underline"
              style={{ color: SEEDHOUSE_COLORS.cream }}
            >
              {t.navAlmanac}
            </a>
          </nav>

          <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
            <button
              type="button"
              onClick={() => setLocale(nextLocale)}
              aria-label={toggleAria}
              className="min-h-[44px] px-2.5 sm:px-3.5 py-2 text-xs font-bold tracking-[0.1em] sm:tracking-[0.12em] uppercase border cursor-pointer transition-colors whitespace-nowrap"
              style={{
                borderColor: '#CFC1A5',
                backgroundColor: '#363E20',
                color: SEEDHOUSE_COLORS.cream,
              }}
            >
              <span className={locale === 'vi' ? 'underline font-extrabold' : 'opacity-80'}>VN</span>
              <span className="mx-1 opacity-60">|</span>
              <span className={locale === 'en' ? 'underline font-extrabold' : 'opacity-80'}>EN</span>
            </button>

            <button
              type="button"
              onClick={() => setAppSheetOpen((prev) => !prev)}
              aria-expanded={appSheetOpen}
              aria-controls="eiu-pdds-app-companion"
              className="min-h-[44px] px-2.5 sm:px-3.5 py-2 text-xs font-bold tracking-[0.1em] sm:tracking-[0.12em] uppercase border cursor-pointer transition-colors whitespace-nowrap"
              style={{
                borderColor: SEEDHOUSE_COLORS.cream,
                backgroundColor: appSheetOpen ? SEEDHOUSE_COLORS.cream : 'transparent',
                color: appSheetOpen ? SEEDHOUSE_COLORS.ink : SEEDHOUSE_COLORS.cream,
              }}
            >
              {t.appButton}
            </button>
          </div>
        </div>
      </div>

      <AnimatePresence initial={false}>
        {appSheetOpen && (
          <m.section
            id="eiu-pdds-app-companion"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
            onAnimationComplete={() => {
              if (!appSheetOpen || !pendingScrollTopRef.current) return;
              pendingScrollTopRef.current = false;
              closeButtonRef.current?.focus({ preventScroll: true });
              if (window.scrollY > 8) {
                scrollToHeaderTop();
              }
            }}
            className="w-full overflow-hidden border-b"
            style={{
              backgroundColor: SEEDHOUSE_COLORS.sheet,
              borderColor: SEEDHOUSE_COLORS.hairline,
              color: SEEDHOUSE_COLORS.ink,
            }}
          >
            <div className="w-full px-5 sm:px-8 lg:px-12 py-6">
              <div className="max-w-7xl mx-auto space-y-4">
                <div className="flex flex-wrap items-baseline justify-between gap-4">
                  <h2 className="font-display text-xl sm:text-2xl font-bold m-0">
                    {t.appSheetTitle}
                  </h2>
                  <button
                    ref={closeButtonRef}
                    type="button"
                    onClick={() => setAppSheetOpen(false)}
                    className="min-h-[44px] px-4 py-2 text-xs font-bold uppercase tracking-[0.1em] border cursor-pointer transition-all duration-200 active:translate-y-[1px]"
                    style={{
                      borderColor: SEEDHOUSE_COLORS.hairline,
                      backgroundColor: SEEDHOUSE_COLORS.parchment,
                      color: SEEDHOUSE_COLORS.ink,
                    }}
                  >
                    {t.appSheetClose}
                  </button>
                </div>

                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-5">
                  <p className="text-base m-0 max-w-3xl" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
                    {t.appSheetBody}
                  </p>

                  {downloadUrl ? (
                    <div className="flex flex-wrap items-center gap-4 shrink-0">
                      <a
                        href={downloadUrl}
                        download
                        className="plaque-terracotta min-h-[44px] px-5 py-2.5 text-xs font-bold uppercase tracking-[0.12em] inline-flex items-center gap-2 no-underline"
                      >
                        {t.appDownloadApkButton}
                      </a>
                      <div
                        data-testid="mobile-app-qr"
                        className="flex items-center gap-3 px-3 py-2 border"
                        style={{
                          borderColor: SEEDHOUSE_COLORS.hairline,
                          backgroundColor: SEEDHOUSE_COLORS.parchment,
                        }}
                      >
                        <QRCode
                          type="svg"
                          value={downloadUrl}
                          size={72}
                          bordered={false}
                          color={SEEDHOUSE_COLORS.ink}
                          bgColor={SEEDHOUSE_COLORS.sheet}
                        />
                        <span className="text-xs font-medium max-w-[14ch] leading-snug" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
                          {t.appQrCaption}
                        </span>
                      </div>
                    </div>
                  ) : (
                    <span
                      className="text-xs font-semibold px-3.5 py-2.5 border shrink-0"
                      style={{
                        borderColor: SEEDHOUSE_COLORS.hairline,
                        backgroundColor: SEEDHOUSE_COLORS.parchment,
                        color: SEEDHOUSE_COLORS.inkMuted,
                      }}
                    >
                      {t.appUnavailableNotice}
                    </span>
                  )}
                </div>

                <div
                  className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-3 border-t"
                  style={{ borderColor: '#D6C8B0' }}
                >
                  <div className="flex items-start gap-3">
                    <StampCheckIcon size={20} stroke={SEEDHOUSE_COLORS.olive} className="shrink-0 mt-1" />
                    <p className="text-sm leading-relaxed m-0">{t.appFeatureHistory}</p>
                  </div>
                  <div className="flex items-start gap-3">
                    <StampCheckIcon size={20} stroke={SEEDHOUSE_COLORS.terracotta} className="shrink-0 mt-1" />
                    <p className="text-sm leading-relaxed m-0">{t.appFeatureExpert}</p>
                  </div>
                  <div className="flex items-start gap-3">
                    <StampCheckIcon size={20} stroke={SEEDHOUSE_COLORS.moderate} className="shrink-0 mt-1" />
                    <p className="text-sm leading-relaxed m-0">{t.appFeatureGps}</p>
                  </div>
                </div>
              </div>
            </div>
          </m.section>
        )}
      </AnimatePresence>

      <DeckledEdge
        fillTop={appSheetOpen ? SEEDHOUSE_COLORS.sheet : SEEDHOUSE_COLORS.olive}
        fillBottom={SEEDHOUSE_COLORS.kraft}
      />
    </header>
  );
}
