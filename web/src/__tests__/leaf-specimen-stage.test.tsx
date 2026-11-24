import { render, screen } from '@testing-library/react';
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
    expect(screen.getByTestId('bbox-overlay-group')).toBeVisible();
    const firstTag = screen.getAllByTestId('bbox-label-pill')[0];
    const rightPct = Number.parseFloat(firstTag.style.right || '0');
    const leftPct = Number.parseFloat(firstTag.style.left || '0');
    const maxW = Number.parseFloat(firstTag.style.maxWidth || '100');
    expect((rightPct || leftPct) + maxW).toBeLessThanOrEqual(99);

    await user.click(screen.getByRole('button', { name: /khung đốm bệnh/i }));
    expect(screen.queryByTestId('bbox-overlay-group')).not.toBeInTheDocument();
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
});
