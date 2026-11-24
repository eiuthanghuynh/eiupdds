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
});
