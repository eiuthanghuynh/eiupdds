'use client';

import React from 'react';
import { SeedhouseHeader } from '@/components/layout/SeedhouseHeader';
import { SeedhouseFooter } from '@/components/layout/SeedhouseFooter';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';

export default function HomePage(): React.JSX.Element {
  const { t } = useLanguage();

  return (
    <div className="min-h-screen flex flex-col bg-kraft-ribbed">
      <SeedhouseHeader />
      <main id="ban-kham" className="flex-1 px-5 sm:px-8 lg:px-12 py-12">
        <div className="max-w-7xl mx-auto space-y-4">
          <h1
            className="font-display text-3xl sm:text-5xl font-bold uppercase tracking-[0.04em] leading-tight m-0"
            style={{ color: SEEDHOUSE_COLORS.ink }}
          >
            {t.heroHeading}
          </h1>
          <hr className="border-0 border-t my-4" style={{ borderColor: SEEDHOUSE_COLORS.hairline }} />
          <p className="text-base sm:text-lg max-w-2xl leading-relaxed m-0" style={{ color: SEEDHOUSE_COLORS.inkMuted }}>
            {t.heroLead}
          </p>
        </div>
      </main>
      <SeedhouseFooter topSurfaceFill={SEEDHOUSE_COLORS.kraft} />
    </div>
  );
}
