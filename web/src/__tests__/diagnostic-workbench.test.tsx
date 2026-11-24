import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { Providers } from '@/app/providers';
import { PrescriptionSpread } from '@/components/diagnosis/PrescriptionSpread';
import { DiagnosticWorkbench } from '@/components/diagnosis/DiagnosticWorkbench';
import { SAMPLE_SPECIMENS } from '@/lib/data/sampleDiagnoses';

describe('PrescriptionSpread & DiagnosticWorkbench', () => {
  it('prioritizes BIOLOGICAL protocol first and switches to CHEMICAL on click', async () => {
    const user = userEvent.setup();
    render(
      <Providers>
        <PrescriptionSpread diagnosis={SAMPLE_SPECIMENS[0].response} />
      </Providers>
    );
    expect(screen.getByRole('tab', { name: /hướng sinh học/i })).toHaveAttribute(
      'aria-selected',
      'true'
    );
    expect(screen.getAllByText(/trichoderma/i).length).toBeGreaterThan(0);

    const chemTab = screen.getByRole('tab', { name: /thuốc đặc trị|chemical/i });
    await user.click(chemTab);
    expect(chemTab).toHaveAttribute('aria-selected', 'true');
    expect(screen.getAllByText(/mancozeb/i).length).toBeGreaterThan(0);
  });

  it('handles healthy leaf or null safetyIntervalDays without rendering "null ngày"', () => {
    const healthyDiag = {
      ...SAMPLE_SPECIMENS[0].response,
      dominantDisease: {
        id: '00000000-0000-0000-0000-000000000099',
        diseaseCode: 'POTATO_HEALTHY',
        commonName: 'Healthy potato leaf',
        scientificName: 'Solanum tuberosum',
        severity: 'LOW' as const,
      },
      recommendedTreatments: [
        {
          id: 't-healthy',
          treatmentType: 'PREVENTIVE' as const,
          title: 'Maintain ventilated root irrigation',
          instructions: 'Water at the root zone in early morning and keep beds well drained.',
          activeIngredients: null,
          dosageRecommendation: null,
          safetyIntervalDays: null,
          warning: null,
        },
      ],
    };
    render(
      <Providers>
        <PrescriptionSpread diagnosis={healthyDiag} />
      </Providers>
    );
    expect(screen.queryByText(/null/i)).not.toBeInTheDocument();
    expect(screen.getByText(/0 ngày|thu hoạch bình thường/i)).toBeInTheDocument();
  });

  it('switches active sample specimen immediately when clicking sample triggers in DiagnosticWorkbench', async () => {
    const user = userEvent.setup();
    render(
      <Providers>
        <DiagnosticWorkbench />
      </Providers>
    );
    await user.click(screen.getByRole('button', { name: /lúa đạo ôn/i }));
    expect(screen.getAllByText(/đạo ôn lúa/i).length).toBeGreaterThan(0);
  });

  it('omits capture attribute on file input for gallery picking and hides stale prescription on upload error', async () => {
    const user = userEvent.setup({ applyAccept: false });
    const { container } = render(
      <Providers>
        <DiagnosticWorkbench />
      </Providers>
    );
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;
    expect(fileInput).not.toBeNull();
    expect(fileInput).not.toHaveAttribute('capture');

    const invalidPdf = new File(['%PDF'], 'bad.pdf', { type: 'application/pdf' });
    await user.upload(fileInput, invalidPdf);

    expect(
      screen.queryByRole('heading', { name: /hướng xử lý & thời gian cách ly/i })
    ).not.toBeInTheDocument();
    expect(screen.getByText(/chỉ hỗ trợ định dạng ảnh jpeg, png hoặc webp/i)).toBeInTheDocument();
  });
});
