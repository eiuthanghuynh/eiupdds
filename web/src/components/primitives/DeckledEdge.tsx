import React from 'react';

export interface DeckledEdgeProps {
  fillTop: string;
  fillBottom: string;
  flip?: boolean;
  className?: string;
}

export function DeckledEdge({
  fillTop,
  fillBottom,
  flip = false,
  className = '',
}: DeckledEdgeProps): React.JSX.Element {
  return (
    <svg
      viewBox="0 0 1440 24"
      preserveAspectRatio="none"
      aria-hidden="true"
      className={`block w-full h-[18px] select-none pointer-events-none ${className}`}
      style={{
        backgroundColor: fillBottom,
        transform: flip ? 'scaleX(-1)' : undefined,
      }}
    >
      <path
        fill={fillTop}
        d="M0,0 L1440,0 L1440,10 L1404,16 L1366,9 L1324,17 L1280,10 L1238,16 L1194,8 L1148,15 L1102,9 L1056,17 L1010,10 L965,16 L918,8 L872,16 L826,9 L780,17 L734,9 L688,15 L642,8 L596,16 L550,9 L504,16 L458,8 L412,15 L366,9 L320,17 L274,9 L228,16 L182,8 L136,15 L90,9 L44,16 L14,10 L0,13 Z"
      />
    </svg>
  );
}
