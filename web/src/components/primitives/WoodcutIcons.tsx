import React from 'react';

interface IconProps {
  size?: number;
  stroke?: string;
  className?: string;
  testId?: string;
}

export function WoodcutSunLeafMark({
  size = 42,
  stroke = '#F6EFE0',
  className = '',
}: IconProps & { centerCutFill?: string }): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 48 48"
      fill="none"
      stroke={stroke}
      aria-hidden="true"
      className={className}
    >
      <circle cx="24" cy="24" r="21" strokeWidth="1.5" />
      <circle cx="24" cy="24" r="17.5" strokeWidth="0.9" strokeDasharray="2.2 2.2" />
      <path
        d="M24 3v4M24 41v4M3 24h4M41 24h4M9.2 9.2l2.8 2.8M36 36l2.8 2.8M38.8 9.2L36 12M12 36l-2.8 2.8M15.8 5.2l1.5 3.4M30.7 39.4l1.5 3.4M42.8 15.8l-3.4 1.5M8.6 30.7l-3.4 1.5"
        strokeWidth="1.3"
        strokeLinecap="round"
      />
      <path
        d="M23.5 34.5c-6.2-3.2-8.2-9.4-4-15.4 3.1-4.2 8.2-5.2 10.6-5.2-.2 6.4-1.4 14.4-6.6 20.6Z"
        fill={stroke}
        stroke="none"
      />
      <path d="M19.8 31.2c3-4.2 5.2-9.2 7.2-14.2" stroke="#434C28" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

export function UploadArrowIcon({
  size = 18,
  stroke = 'currentColor',
  className = '',
}: IconProps): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 20 20"
      fill="none"
      stroke={stroke}
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
    >
      <path d="M10 13.5V3.5M10 3.5L6 7.5M10 3.5l4 4" />
      <path d="M3.5 13.5v2a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1v-2" />
    </svg>
  );
}

export function CameraLeafIcon({
  size = 22,
  stroke = 'currentColor',
  className = '',
}: IconProps): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke={stroke}
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
    >
      <path d="M3 8a2 2 0 0 1 2-2h2.2l1.4-2h6.8l1.4 2H19a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8Z" />
      <path d="M9.5 15.5c-.8-2.6.6-5.2 3.8-6 .4 2.8-.8 5.4-3.8 6Z" />
      <path d="M9.5 15.5L13 12" />
    </svg>
  );
}

export function EyeReticleIcon({
  size = 16,
  stroke = 'currentColor',
  className = '',
}: IconProps): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 20 20"
      fill="none"
      stroke={stroke}
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
    >
      <path d="M2 10s3-5 8-5 8 5 8 5-3 5-8 5-8-5-8-5Z" />
      <circle cx="10" cy="10" r="2.3" />
    </svg>
  );
}

export function StampCheckIcon({
  size = 18,
  stroke = 'currentColor',
  className = '',
  testId,
}: IconProps): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 20 20"
      fill="none"
      stroke={stroke}
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      data-testid={testId}
      className={className}
    >
      <circle cx="10" cy="10" r="7.5" strokeDasharray="2.5 2" />
      <path d="M6.8 10.2l2.2 2.2 4.4-4.6" />
    </svg>
  );
}

export function StampAlertIcon({
  size = 18,
  stroke = 'currentColor',
  className = '',
  testId,
}: IconProps): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 20 20"
      fill="none"
      stroke={stroke}
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      data-testid={testId}
      className={className}
    >
      <path d="M10 3L17.5 16.2H2.5L10 3Z" />
      <path d="M10 8v4" />
      <circle cx="10" cy="14" r="0.6" fill={stroke} />
    </svg>
  );
}

export function CropGlyphIcon({
  size = 18,
  stroke = 'currentColor',
  className = '',
}: IconProps): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 20 20"
      fill="none"
      stroke={stroke}
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
    >
      <path d="M10 17V5" />
      <path d="M10 12c-3.5 0-5.5-2-5.5-5 3 0 5.5 1.8 5.5 5Z" />
      <path d="M10 9.5c3.5 0 5.5-2 5.5-5-3 0-5.5 1.8-5.5 5Z" />
    </svg>
  );
}

export function ChevronFoldIcon({
  size = 16,
  stroke = 'currentColor',
  open = false,
  className = '',
}: IconProps & { open?: boolean }): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 20 20"
      fill="none"
      stroke={stroke}
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
      style={{
        transform: open ? 'rotate(180deg)' : 'rotate(0deg)',
        transition: 'transform 180ms cubic-bezier(0.22, 1, 0.36, 1)',
      }}
    >
      <path d="M5 7.5l5 5 5-5" />
    </svg>
  );
}

export function SearchLoupeIcon({
  size = 16,
  stroke = 'currentColor',
  className = '',
}: IconProps): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 20 20"
      fill="none"
      stroke={stroke}
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
    >
      <circle cx="8.8" cy="8.8" r="5.3" />
      <path d="M13 13l3.8 3.8" />
    </svg>
  );
}
