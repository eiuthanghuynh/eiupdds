import { render, screen, waitFor } from '@testing-library/react';
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
    await user.click(screen.getByRole('button', { name: /táo gỉ sắt/i }));
    expect(screen.getAllByText(/gỉ sắt lá táo/i).length).toBeGreaterThan(0);
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

  it('renders AntD Seedhouse crop select below CTA and balanced 3-column treatment spread without JPEG/PNG/WebP tech copy', () => {
    const { container } = render(
      <Providers>
        <DiagnosticWorkbench />
      </Providers>
    );
    const antSelect = container.querySelector('.ant-select .ant-select-selector');
    expect(antSelect).not.toBeNull();
    expect(container.querySelector('.lg\\:grid-cols-3')).not.toBeNull();
    expect(screen.queryByText(/JPEG, PNG, WebP/i)).not.toBeInTheDocument();
    expect(screen.getByText(/1 lá đủ sáng/i)).toBeInTheDocument();
  });

  it('localizes English live-upload treatments to Vietnamese, warns about chemical wait days on 0-day bio tab, and omits dosage label on CULTURAL tab', async () => {
    const user = userEvent.setup();
    const liveEnglishDiag = {
      ...SAMPLE_SPECIMENS[0].response,
      diagnosisId: '99999999-9999-9999-9999-999999999999',
    };
    render(
      <Providers>
        <PrescriptionSpread diagnosis={liveEnglishDiag} />
      </Providers>
    );
    expect(screen.getByText(/vệ sinh tán lá & phun vi sinh đối kháng/i)).toBeInTheDocument();
    expect(screen.getByText(/nếu chuyển sang thuốc đặc trị.*7 ngày/i)).toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: /chăm sóc & canh tác/i }));
    expect(screen.queryByText(/liều lượng tham khảo/i)).not.toBeInTheDocument();
  });

  it('moves shared layoutId highlight across treatment tabs and counts harvest safety seal on Thuốc đặc trị', async () => {
    const user = userEvent.setup();
    render(
      <Providers>
        <PrescriptionSpread diagnosis={SAMPLE_SPECIMENS[0].response} />
      </Providers>
    );

    const bioTab = screen.getByRole('tab', { name: /hướng sinh học/i });
    expect(bioTab.querySelector('[data-testid="prescription-active-indicator"]')).not.toBeNull();

    const chemTab = screen.getByRole('tab', { name: /thuốc đặc trị/i });
    await user.click(chemTab);
    expect(chemTab.querySelector('[data-testid="prescription-active-indicator"]')).not.toBeNull();
    expect(bioTab.querySelector('[data-testid="prescription-active-indicator"]')).toBeNull();

    await waitFor(() => {
      expect(screen.getByTestId('harvest-day-seal')).toHaveTextContent(/07/);
    });
  });
});
