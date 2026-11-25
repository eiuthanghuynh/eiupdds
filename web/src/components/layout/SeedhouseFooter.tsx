'use client';

import React from 'react';
import { DeckledEdge } from '@/components/primitives/DeckledEdge';
import { EiuUniversityMark } from '@/components/primitives/EiuUniversityMark';
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
        className="w-full px-5 sm:px-8 lg:px-12 py-8"
        style={{ backgroundColor: SEEDHOUSE_COLORS.olive, color: SEEDHOUSE_COLORS.cream }}
      >
        <div className="max-w-7xl mx-auto flex flex-col lg:flex-row lg:items-center justify-between gap-5">
          <div className="flex items-center gap-3 max-w-md">
            <EiuUniversityMark size={32} />
            <p className="text-sm leading-snug m-0" style={{ color: '#EAE0CC' }}>
              {t.footerSummary}
            </p>
          </div>

          <div
            className="lg:pl-6 lg:border-l space-y-1 max-w-md"
            style={{ borderColor: SEEDHOUSE_COLORS.oliveRule }}
          >
            <p className="text-xs leading-snug m-0" style={{ color: '#EAE0CC' }}>
              {t.footerSafetyNote}
            </p>
            <p className="text-xs m-0" style={{ color: '#D8CCB4' }}>
              {t.footerCopyright}
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
}
