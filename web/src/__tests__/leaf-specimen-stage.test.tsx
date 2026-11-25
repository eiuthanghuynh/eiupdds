import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Providers } from '@/app/providers';
import { LeafSpecimenStage } from '@/components/diagnosis/LeafSpecimenStage';
import { SAMPLE_SPECIMENS } from '@/lib/data/sampleDiagnoses';
import { PddsApiError } from '@/lib/api/client';

describe('LeafSpecimenStage', () => {
  it('locks stage aspect-ratio to portrait image dimensions and toggles bounding boxes', async () => {
    const user = userEvent.setup();
    const portraitDiag = {
      ...SAMPLE_SPECIMENS[0].response,
      imageWidth: 1080,
      imageHeight: 1920,
      boundingBoxes: [
        {
          ...SAMPLE_SPECIMENS[0].response.boundingBoxes[0],
          xMinNormalized: 0.48,
          yMinNormalized: 0.28,
          widthNormalized: 0.18,
          heightNormalized: 0.24,
        },
        {
          ...SAMPLE_SPECIMENS[0].response.boundingBoxes[1],
          xMinNormalized: 0.35,
          yMinNormalized: 0.53,
          widthNormalized: 0.1,
          heightNormalized: 0.11,
        },
      ],
    };
    render(
      <Providers>
        <LeafSpecimenStage
          diagnosis={portraitDiag}
          isSample={true}
          isLoading={false}
          error={null}
          onRetryUpload={vi.fn()}
          onSelectSample={vi.fn()}
        />
      </Providers>
    );
    const frame = screen.getByTestId('specimen-aspect-frame');
    expect(frame).toHaveStyle({ aspectRatio: '1080 / 1920' });
    const overlayGroup = screen.getByTestId('bbox-overlay-group');
    expect(overlayGroup).toBeVisible();
    expect(overlayGroup).not.toHaveClass('animate-bbox-reveal');
    expect(screen.queryByText('EIU PDDS')).not.toBeInTheDocument();
    const pills = screen.getAllByTestId('bbox-label-pill');
    const firstTag = pills[0];
    const rightPct = Number.parseFloat(firstTag.style.right || '0');
    const leftPct = Number.parseFloat(firstTag.style.left || '0');
    const maxW = Number.parseFloat(firstTag.style.maxWidth || '100');
    expect((rightPct || leftPct) + maxW).toBeLessThanOrEqual(99);
    expect(pills[0]).toHaveAttribute('data-placement', 'above');
    expect(pills[1]).toHaveAttribute('data-placement', 'below');

    await user.click(screen.getByRole('button', { name: /khung đốm bệnh/i }));
    await waitFor(() => {
      expect(screen.queryByTestId('bbox-overlay-group')).not.toBeInTheDocument();
    });
  });

  it('staggers real 4-box Potato Late Blight pills to distinct vertical bands without sharing top% across boxes', () => {
    render(
      <Providers>
        <LeafSpecimenStage
          diagnosis={SAMPLE_SPECIMENS[0].response}
          isSample={true}
          isLoading={false}
          error={null}
          onRetryUpload={vi.fn()}
          onSelectSample={vi.fn()}
        />
      </Providers>
    );
    const pills = screen.getAllByTestId('bbox-label-pill');
    expect(pills).toHaveLength(4);
    const tops = pills.map((p) => Number.parseFloat(p.style.top || '0'));
    for (let i = 0; i < tops.length; i += 1) {
      for (let j = i + 1; j < tops.length; j += 1) {
        expect(Math.abs(tops[i] - tops[j])).toBeGreaterThanOrEqual(12);
      }
    }
  });

  it('renders StampAlertIcon on CRITICAL severity, displays disease names on lesion pills, hides Latin subtitle, and keeps preview thumbnail on 422', () => {
    const { rerender } = render(
      <Providers>
        <LeafSpecimenStage
          diagnosis={SAMPLE_SPECIMENS[0].response}
          isSample={true}
          isLoading={false}
          error={null}
          onRetryUpload={vi.fn()}
          onSelectSample={vi.fn()}
        />
      </Providers>
    );
    expect(screen.getByTestId('severity-alert-icon')).toBeInTheDocument();
    expect(screen.queryByTestId('severity-check-icon')).not.toBeInTheDocument();
    const pills = screen.getAllByTestId('bbox-label-pill');
    expect(pills[0]).toHaveTextContent(/bệnh mốc sương khoai tây.*86%/i);
    expect(screen.queryByText('Phytophthora infestans')).not.toBeInTheDocument();

    rerender(
      <Providers>
        <LeafSpecimenStage
          diagnosis={SAMPLE_SPECIMENS[0].response}
          previewUrl="blob:mock-blurry-leaf"
          isSample={false}
          isLoading={false}
          error={new PddsApiError(422, 'DIAGNOSIS_003', 'No leaf found')}
          onRetryUpload={vi.fn()}
          onSelectSample={vi.fn()}
        />
      </Providers>
    );
    const thumb = screen.getByTestId('error-422-preview');
    expect(thumb).toHaveAttribute('src', 'blob:mock-blurry-leaf');
  });

  it('renders plain-Vietnamese retake guidance on 422 DIAGNOSIS_003 and sample buttons on 429 RATE_001', async () => {
    const onSelectSample = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(
      <Providers>
        <LeafSpecimenStage
          diagnosis={SAMPLE_SPECIMENS[0].response}
          isSample={false}
          isLoading={false}
          error={new PddsApiError(422, 'DIAGNOSIS_003', 'No leaf found')}
          onRetryUpload={vi.fn()}
          onSelectSample={onSelectSample}
        />
      </Providers>
    );
    expect(screen.getByText(/chưa nhìn rõ lá cây hoặc vết bệnh/i)).toBeInTheDocument();
    expect(screen.queryByText(/nguy hiểm/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/bệnh mốc sương khoai tây/i)).not.toBeInTheDocument();

    rerender(
      <Providers>
        <LeafSpecimenStage
          diagnosis={SAMPLE_SPECIMENS[0].response}
          isSample={false}
          isLoading={false}
          error={new PddsApiError(429, 'RATE_001', 'Quota exceeded', 1200)}
          onRetryUpload={vi.fn()}
          onSelectSample={onSelectSample}
        />
      </Providers>
    );
    expect(screen.getByText(/3 lượt quét ảnh thật/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /khoai tây/i }));
    expect(onSelectSample).toHaveBeenCalledWith(0);
  });

  it('draws SVG reticles, spotlights active lesion pill while dimming siblings, resets on specimen change, and shows lamp sweep while loading', async () => {
    const user = userEvent.setup();
    const { container, rerender } = render(
      <Providers>
        <LeafSpecimenStage
          diagnosis={SAMPLE_SPECIMENS[0].response}
          isSample={true}
          isLoading={false}
          error={null}
          onRetryUpload={vi.fn()}
          onSelectSample={vi.fn()}
        />
      </Providers>
    );

    const rects = container.querySelectorAll('svg rect');
    expect(rects.length).toBe(4);
    expect(rects[0].getAttribute('class') ?? '').toContain('animate-reticle-draw');

    const pills = screen.getAllByTestId('bbox-label-pill');
    await user.hover(pills[0]);
    expect(pills[0]).toHaveAttribute('data-highlighted', 'true');
    expect(pills[1]).toHaveAttribute('data-dimmed', 'true');
    expect(rects[1]).toHaveAttribute('opacity', '0.28');

    await user.unhover(pills[0]);
    expect(pills[0]).not.toHaveAttribute('data-highlighted', 'true');
    expect(pills[1]).not.toHaveAttribute('data-dimmed', 'true');

    // Reset highlight if diagnosis switches while active
    await user.hover(pills[0]);
    expect(pills[0]).toHaveAttribute('data-highlighted', 'true');
    rerender(
      <Providers>
        <LeafSpecimenStage
          diagnosis={SAMPLE_SPECIMENS[1].response}
          isSample={true}
          isLoading={false}
          error={null}
          onRetryUpload={vi.fn()}
          onSelectSample={vi.fn()}
        />
      </Providers>
    );
    const updatedPills = screen.getAllByTestId('bbox-label-pill');
    expect(updatedPills[0]).not.toHaveAttribute('data-highlighted', 'true');

    // Warm inspection lamp sweep during loading
    rerender(
      <Providers>
        <LeafSpecimenStage
          diagnosis={SAMPLE_SPECIMENS[0].response}
          isSample={false}
          isLoading={true}
          error={null}
          onRetryUpload={vi.fn()}
          onSelectSample={vi.fn()}
        />
      </Providers>
    );
    expect(screen.getByTestId('specimen-lamp-sweep')).toBeInTheDocument();
  });
});
