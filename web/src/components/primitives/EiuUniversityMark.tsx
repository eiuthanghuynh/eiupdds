import React from 'react';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';

export interface EiuUniversityMarkProps {
  size?: number;
  className?: string;
}

export function EiuUniversityMark({
  size = 34,
  className = '',
}: EiuUniversityMarkProps): React.JSX.Element {
  return (
    <span
      className={`inline-flex items-center justify-center shrink-0 rounded-full border p-1 ${className}`}
      style={{
        width: size,
        height: size,
        backgroundColor: SEEDHOUSE_COLORS.sheet,
        borderColor: '#CFC1A5',
      }}
    >
      <img
        data-testid="eiu-brand-mark"
        src="/eiu-mark.png"
        alt=""
        aria-hidden="true"
        className="w-full h-full object-contain select-none"
      />
    </span>
  );
}
