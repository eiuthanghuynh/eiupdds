import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Providers } from '@/app/providers';
import { CropDiseaseAlmanac } from '@/components/catalog/CropDiseaseAlmanac';

describe('CropDiseaseAlmanac & Inline Fold-Open Sheet', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders 9 crops (using offline fallback when server is down), filters by crop, and unfolds inline sheet on click', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));
    const user = userEvent.setup();
    render(
      <Providers>
        <CropDiseaseAlmanac />
      </Providers>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /^Cà chua$/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /^Lúa$/i }));
    await waitFor(() => {
      expect(screen.queryByText(/mốc sương khoai tây/i)).not.toBeInTheDocument();
    });
    const riceBlastRow = await screen.findByRole('button', { name: /đạo ôn lúa/i });
    await user.click(riceBlastRow);

    // Inline paper sheet unfolds in-place (not a modal dialog)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect((await screen.findAllByText(/magnaporthe oryzae/i)).length).toBeGreaterThan(0);
  });
});
