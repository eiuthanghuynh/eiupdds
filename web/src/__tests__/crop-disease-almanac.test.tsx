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
    expect(screen.queryByRole('button', { name: /^Việt quất$/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Dâu tây$/i })).not.toBeInTheDocument();
    expect(screen.getByText(/đào.*đậu nành.*việt quất.*anh đào.*mâm xôi.*bí ngô.*dâu tây/i)).toBeInTheDocument();

    const searchBox = await screen.findByPlaceholderText(/mốc sương/i);
    await user.type(searchBox, 'moc suong');
    expect(await screen.findByRole('button', { name: /mốc sương khoai tây/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /mốc sương cà chua/i })).toBeInTheDocument();
    expect(screen.getAllByTestId('almanac-disease-row')).toHaveLength(2);
  });

  it('excludes healthy leaf entries even when returned by live backend API', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes('/api/v1/plants')) {
        return new Response(
          JSON.stringify({
            success: true,
            data: [
              { id: 'p1', plantCode: 'POTATO', commonName: 'Potato' },
              { id: 'p2', plantCode: 'BLUEBERRY', commonName: 'Blueberry' },
              { id: 'p3', plantCode: 'CHERRY', commonName: 'Cherry' },
              { id: 'p4', plantCode: 'RASPBERRY', commonName: 'Raspberry' },
              { id: 'p5', plantCode: 'SQUASH', commonName: 'Squash' },
              { id: 'p6', plantCode: 'STRAWBERRY', commonName: 'Strawberry' },
              { id: 'p7', plantCode: 'PEACH', commonName: 'Peach' },
              { id: 'p8', plantCode: 'SOYBEAN', commonName: 'Soybean' },
            ],
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } }
        );
      }
      return new Response(
        JSON.stringify({
          success: true,
          data: {
            content: [
              {
                id: 'd1',
                diseaseCode: 'POTATO_LATE_BLIGHT',
                commonName: 'Potato late blight',
                severity: 'CRITICAL',
              },
              {
                id: 'd2',
                diseaseCode: 'POTATO_HEALTHY',
                commonName: 'Healthy potato leaf',
                severity: 'LOW',
              },
              {
                id: 'd3',
                diseaseCode: 'BLUEBERRY_HEALTHY',
                commonName: 'Healthy blueberry leaf',
                severity: null,
              },
            ],
          },
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      );
    });

    render(
      <Providers>
        <CropDiseaseAlmanac />
      </Providers>
    );

    expect(await screen.findByRole('button', { name: /mốc sương khoai tây/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /khỏe mạnh/i })).not.toBeInTheDocument();
    expect(screen.getAllByTestId('almanac-disease-row')).toHaveLength(1);
    expect(screen.queryByRole('button', { name: /^Blueberry$/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Việt quất$/i })).not.toBeInTheDocument();
    expect(screen.getByText(/việt quất.*anh đào.*mâm xôi.*bí ngô.*dâu tây/i)).toBeInTheDocument();
  });
});
