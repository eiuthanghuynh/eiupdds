'use client';

import React, { useState } from 'react';
import { DeckledEdge } from '@/components/primitives/DeckledEdge';
import { StampCheckIcon, WoodcutSunLeafMark } from '@/components/primitives/WoodcutIcons';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';

export function SeedhouseHeader(): React.JSX.Element {
  const { locale, setLocale, t } = useLanguage();
  const [appSheetOpen, setAppSheetOpen] = useState(false);

  const nextLocale = locale === 'vi' ? 'en' : 'vi';
  const toggleAria = locale === 'vi' ? t.langToggleAriaToEn : t.langToggleAriaToVi;

  return (
    <header className="w-full">
      <div
        className="w-full px-5 sm:px-8 lg:px-12 pt-5 pb-4"
        style={{ backgroundColor: SEEDHOUSE_COLORS.olive, color: SEEDHOUSE_COLORS.cream }}
      >
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          <a
            href="#ban-kham"
            className="flex items-center gap-3.5 no-underline group"
            style={{ color: SEEDHOUSE_COLORS.cream }}
          >
            <WoodcutSunLeafMark size={42} stroke={SEEDHOUSE_COLORS.cream} />
            <div>
              <div className="font-display text-lg sm:text-xl font-bold tracking-[0.2em] uppercase leading-tight">
                EIU PDDS
              </div>
              <div
                className="text-xs tracking-[0.08em] uppercase"
                style={{ color: '#E2D6BE' }}
              >
                {t.brandSubtitle}
              </div>
            </div>
          </a>

          <nav
            aria-label="Điều hướng trang chủ"
            className="hidden md:flex items-center gap-8 text-sm font-medium tracking-[0.1em] uppercase"
          >
            <a
              href="#ban-kham"
              className="hover:underline"
              style={{ color: SEEDHOUSE_COLORS.cream }}
            >
              {t.navWorkbench}
            </a>
            <a
              href="#quy-trinh"
              className="hover:underline"
              style={{ color: SEEDHOUSE_COLORS.cream }}
            >
              {t.navHowItWorks}
            </a>
            <a
              href="#so-tay"
              className="hover:underline"
              style={{ color: SEEDHOUSE_COLORS.cream }}
            >
              {t.navAlmanac}
            </a>
          </nav>

          <div className="flex items-center gap-2.5">
            <button
              type="button"
              onClick={() => setLocale(nextLocale)}
              aria-label={toggleAria}
              className="px-3.5 py-2 text-xs font-bold tracking-[0.12em] uppercase border cursor-pointer transition-colors"
              style={{
                borderColor: '#CFC1A5',
                backgroundColor: '#363E20',
                color: SEEDHOUSE_COLORS.cream,
              }}
            >
              <span className={locale === 'vi' ? 'underline font-extrabold' : 'opacity-80'}>VN</span>
              <span className="mx-1.5 opacity-60">|</span>
              <span className={locale === 'en' ? 'underline font-extrabold' : 'opacity-80'}>EN</span>
            </button>

            <button
              type="button"
              onClick={() => setAppSheetOpen((prev) => !prev)}
              aria-expanded={appSheetOpen}
              aria-controls="eiu-pdds-app-companion"
              className="px-4 py-2 text-xs font-bold tracking-[0.12em] uppercase border cursor-pointer transition-colors"
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

      {appSheetOpen && (
        <section
          id="eiu-pdds-app-companion"
          className="w-full px-5 sm:px-8 lg:px-12 py-6 border-b"
          style={{
            backgroundColor: SEEDHOUSE_COLORS.sheet,
            borderColor: SEEDHOUSE_COLORS.hairline,
            color: SEEDHOUSE_COLORS.ink,
          }}
        >
          <div className="max-w-7xl mx-auto space-y-4">
            <div className="flex flex-wrap items-baseline justify-between gap-4">
              <h2 className="font-display text-xl sm:text-2xl font-bold m-0">
                {t.appSheetTitle}
              </h2>
              <button
                type="button"
                onClick={() => setAppSheetOpen(false)}
                className="px-3.5 py-1.5 text-xs font-bold uppercase tracking-[0.1em] border cursor-pointer"
                style={{
                  borderColor: SEEDHOUSE_COLORS.hairline,
                  backgroundColor: SEEDHOUSE_COLORS.parchment,
                  color: SEEDHOUSE_COLORS.ink,
                }}
              >
                {t.appSheetClose}
              </button>
            </div>
            <p className="text-base m-0 max-w-4xl" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
              {t.appSheetBody}
            </p>
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
        </section>
      )}

      <DeckledEdge
        fillTop={appSheetOpen ? SEEDHOUSE_COLORS.sheet : SEEDHOUSE_COLORS.olive}
        fillBottom={SEEDHOUSE_COLORS.kraft}
      />
    </header>
  );
}
