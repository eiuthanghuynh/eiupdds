'use client';

import React from 'react';
import { DeckledEdge } from '@/components/primitives/DeckledEdge';
import { WoodcutSunLeafMark } from '@/components/primitives/WoodcutIcons';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';

export function SeedhouseFooter({
  topSurfaceFill = SEEDHOUSE_COLORS.parchment,
}: {
  topSurfaceFill?: string;
}): React.JSX.Element {
  const { t } = useLanguage();

  return (
    <footer className="w-full">
      <DeckledEdge fillTop={topSurfaceFill} fillBottom={SEEDHOUSE_COLORS.olive} flip />
      <div
        className="w-full px-5 sm:px-8 lg:px-12 py-10"
        style={{ backgroundColor: SEEDHOUSE_COLORS.olive, color: SEEDHOUSE_COLORS.cream }}
      >
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          <div className="lg:col-span-6 space-y-3">
            <div className="flex items-center gap-3">
              <WoodcutSunLeafMark size={34} stroke={SEEDHOUSE_COLORS.cream} />
              <span className="font-display text-base font-bold tracking-[0.18em] uppercase">
                EIU PDDS
              </span>
            </div>
            <p className="text-sm leading-relaxed m-0 max-w-xl" style={{ color: '#EAE0CC' }}>
              {t.footerSummary}
            </p>
          </div>

          <div
            className="lg:col-span-6 lg:pl-8 lg:border-l space-y-3"
            style={{ borderColor: SEEDHOUSE_COLORS.oliveRule }}
          >
            <p className="text-sm leading-relaxed m-0" style={{ color: '#EAE0CC' }}>
              {t.footerSafetyNote}
            </p>
            <p className="text-xs tracking-[0.06em] m-0 pt-2" style={{ color: '#D8CCB4' }}>
              {t.footerCopyright}
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
}
