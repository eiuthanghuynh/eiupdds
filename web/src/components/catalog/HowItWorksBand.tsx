'use client';

import React from 'react';
import { DeckledEdge } from '@/components/primitives/DeckledEdge';
import {
  CameraLeafIcon,
  EyeReticleIcon,
  StampCheckIcon,
} from '@/components/primitives/WoodcutIcons';
import { useLanguage } from '@/lib/i18n/LanguageContext';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';

export function HowItWorksBand(): React.JSX.Element {
  const { t } = useLanguage();

  return (
    <section id="quy-trinh" className="w-full">
      <DeckledEdge
        fillTop={SEEDHOUSE_COLORS.parchment}
        fillBottom={SEEDHOUSE_COLORS.olive}
        flip
      />

      <div
        className="w-full px-5 sm:px-8 lg:px-12 py-12"
        style={{ backgroundColor: SEEDHOUSE_COLORS.olive, color: SEEDHOUSE_COLORS.cream }}
      >
        <div className="max-w-7xl mx-auto space-y-8">
          <div
            className="pb-5 border-b space-y-2"
            style={{ borderColor: SEEDHOUSE_COLORS.oliveRule }}
          >
            <h2 className="font-display text-2xl sm:text-3xl font-bold m-0">
              {t.howHeading}
            </h2>
            <p className="text-base max-w-3xl leading-relaxed m-0" style={{ color: '#E8DEC8' }}>
              {t.howLead}
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div
              className="lg:pr-8 lg:border-r space-y-3"
              style={{ borderColor: SEEDHOUSE_COLORS.oliveRule }}
            >
              <div className="w-11 h-11 rounded-full border border-dashed flex items-center justify-center" style={{ borderColor: SEEDHOUSE_COLORS.mustard }}>
                <CameraLeafIcon size={22} stroke={SEEDHOUSE_COLORS.mustard} />
              </div>
              <h3 className="font-display text-xl font-bold leading-snug m-0">
                {t.howStep1Title}
              </h3>
              <p className="text-base leading-relaxed m-0" style={{ color: '#EAE0CC' }}>
                {t.howStep1Body}
              </p>
            </div>

            <div
              className="lg:pr-8 lg:border-r space-y-3"
              style={{ borderColor: SEEDHOUSE_COLORS.oliveRule }}
            >
              <div className="w-11 h-11 rounded-full border border-dashed flex items-center justify-center" style={{ borderColor: SEEDHOUSE_COLORS.mustard }}>
                <EyeReticleIcon size={20} stroke={SEEDHOUSE_COLORS.mustard} />
              </div>
              <h3 className="font-display text-xl font-bold leading-snug m-0">
                {t.howStep2Title}
              </h3>
              <p className="text-base leading-relaxed m-0" style={{ color: '#EAE0CC' }}>
                {t.howStep2Body}
              </p>
            </div>

            <div className="space-y-3">
              <div className="w-11 h-11 rounded-full border border-dashed flex items-center justify-center" style={{ borderColor: SEEDHOUSE_COLORS.mustard }}>
                <StampCheckIcon size={20} stroke={SEEDHOUSE_COLORS.mustard} />
              </div>
              <h3 className="font-display text-xl font-bold leading-snug m-0">
                {t.howStep3Title}
              </h3>
              <p className="text-base leading-relaxed m-0" style={{ color: '#EAE0CC' }}>
                {t.howStep3Body}
              </p>
            </div>
          </div>
        </div>
      </div>

      <DeckledEdge
        fillTop={SEEDHOUSE_COLORS.olive}
        fillBottom={SEEDHOUSE_COLORS.parchment}
      />
    </section>
  );
}
