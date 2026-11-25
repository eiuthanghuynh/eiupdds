import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Providers } from '@/app/providers';
import { CropDiseaseAlmanac } from '@/components/catalog/CropDiseaseAlmanac';

describe('CropDiseaseAlmanac & Inline Fold-Open Sheet', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows 6 disease rows by default, hides Latin binomials when collapsed, expands to 15, and unfolds/closes sheet with dosage on click & Escape', async () => {
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

    expect(screen.getAllByTestId('almanac-disease-row')).toHaveLength(6);
    expect(screen.queryByText(/magnaporthe oryzae/i)).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /xem tất cả/i }));
    expect(screen.getAllByTestId('almanac-disease-row')).toHaveLength(15);

    await user.click(screen.getByRole('button', { name: /^Lúa$/i }));
    await waitFor(() => {
      expect(screen.queryByText(/mốc sương khoai tây/i)).not.toBeInTheDocument();
    });

    const riceBlastRow = await screen.findByRole('button', { name: /đạo ôn lúa/i });
    await user.click(riceBlastRow);

    // Inline paper sheet unfolds in-place (not a modal dialog) with active ingredients & dosage
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(await screen.findByText(/magnaporthe oryzae/i)).toBeInTheDocument();
    expect(screen.getAllByText(/hoạt chất/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/tricyclazole/i).length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText(/liều lượng/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/30–40g/i)).toBeInTheDocument();

    await user.keyboard('{Escape}');
    await waitFor(() => {
      expect(screen.queryByText(/magnaporthe oryzae/i)).not.toBeInTheDocument();
    });
  });

  it('supports accentless Vietnamese search ("moc suong") and omits empty crop buttons with a concise note', async () => {
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

    expect(screen.queryByRole('button', { name: /^Đào$/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Đậu nành$/i })).not.toBeInTheDocument();
    expect(screen.getByText(/đào.*đậu nành/i)).toBeInTheDocument();

    const searchBox = await screen.findByPlaceholderText(/mốc sương/i);
    await user.type(searchBox, 'moc suong');
    expect(await screen.findByRole('button', { name: /mốc sương khoai tây/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /mốc sương cà chua/i })).toBeInTheDocument();
  });
});
