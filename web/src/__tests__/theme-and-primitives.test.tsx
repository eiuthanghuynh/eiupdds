import fs from 'node:fs';
import path from 'node:path';
import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SEEDHOUSE_COLORS, seedhouseAntdTheme } from '@/lib/theme/seedhouseTheme';
import { DeckledEdge } from '@/components/primitives/DeckledEdge';
import { WoodcutSunLeafMark } from '@/components/primitives/WoodcutIcons';

describe('Seedhouse Theme & Craft Primitives', () => {
  it('pins WCAG-compliant Village Seedhouse palette and crisp 2px AntD radius', () => {
    expect(SEEDHOUSE_COLORS.kraft).toBe('#E6DAC4');
    expect(SEEDHOUSE_COLORS.olive).toBe('#434C28');
    expect(SEEDHOUSE_COLORS.terracotta).toBe('#8C3419');
    expect(seedhouseAntdTheme.token?.colorPrimary).toBe('#8C3419');
    expect(seedhouseAntdTheme.token?.borderRadius).toBe(2);
  });

  it('renders SVG deckled edge and woodcut emblem without Unicode emoji', () => {
    const { container } = render(
      <div>
        <DeckledEdge fillTop="#434C28" fillBottom="#E6DAC4" />
        <WoodcutSunLeafMark size={40} />
      </div>
    );
    expect(container.querySelectorAll('svg').length).toBe(2);
    expect(container.textContent).toBe('');
  });

  it('uses Motion domMax for layoutId projection and avoids keyframe opacity overrides on reticle draw', () => {
    const providersSrc = fs.readFileSync(
      path.resolve(process.cwd(), 'src/app/providers.tsx'),
      'utf8'
    );
    expect(providersSrc).toContain('domMax');
    expect(providersSrc).not.toContain('domAnimation');

    const globalsCss = fs.readFileSync(
      path.resolve(process.cwd(), 'src/app/globals.css'),
      'utf8'
    );
    const reticleBlock = globalsCss.match(/@keyframes\s+seedhouseReticleDraw\s*\{[\s\S]*?\n\}/)?.[0] ?? '';
    expect(reticleBlock).toContain('stroke-dashoffset');
    expect(reticleBlock).not.toMatch(/\bopacity\s*:/);
  });
});
