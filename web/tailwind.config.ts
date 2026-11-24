import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        kraft: '#E6DAC4',
        parchment: '#EFE7D6',
        sheet: '#FAF5EB',
        olive: {
          DEFAULT: '#434C28',
          stage: '#3D4624',
          mat: '#2F371B',
          rule: '#697349',
        },
        terracotta: {
          DEFAULT: '#8C3419',
          hover: '#732912',
        },
        mustard: '#F5CF7A',
        critical: '#8F1D1E',
        moderate: '#78350F',
        hairline: '#B8A88A',
        ink: {
          DEFAULT: '#241C14',
          muted: '#4A3D2E',
          cream: '#F6EFE0',
        },
      },
      fontFamily: {
        display: ['var(--font-display)', 'Georgia', 'Cambria', 'serif'],
        body: ['var(--font-body)', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};

export default config;
