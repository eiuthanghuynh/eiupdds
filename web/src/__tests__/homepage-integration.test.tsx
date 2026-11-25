import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Providers } from '@/app/providers';
import HomePage from '@/app/page';

describe('EIU PDDS Full Homepage Integration', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders distilled 3-band Seedhouse layout without HowItWorksBand or modals', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Offline preview'));
    render(
      <Providers>
        <HomePage />
      </Providers>
    );

    expect(screen.getByRole('heading', { level: 1, name: /soi đốm lá/i })).toBeInTheDocument();
    expect(screen.getByTestId('specimen-aspect-frame')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /hướng xử lý & thời gian cách ly/i })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /ba bước khám lá/i })).not.toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /sổ tay bệnh cây trồng/i })).toBeInTheDocument();
    });
  });
});
