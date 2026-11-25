import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Providers } from '@/app/providers';
import { SeedhouseHeader } from '@/components/layout/SeedhouseHeader';
import { PrescriptionSpread } from '@/components/diagnosis/PrescriptionSpread';
import { LeafSpecimenStage } from '@/components/diagnosis/LeafSpecimenStage';
import { SAMPLE_SPECIMENS } from '@/lib/data/sampleDiagnoses';
import { PddsApiError } from '@/lib/api/client';
import { getMobileAppDownloadUrl, openMobileAppDrawer } from '@/lib/config/mobileApp';

describe('Mobile App APK & QR Handoff', () => {
  const originalUrl = process.env.NEXT_PUBLIC_MOBILE_APP_URL;

  afterEach(() => {
    if (originalUrl === undefined) {
      delete process.env.NEXT_PUBLIC_MOBILE_APP_URL;
    } else {
      process.env.NEXT_PUBLIC_MOBILE_APP_URL = originalUrl;
    }
    vi.restoreAllMocks();
  });

  it('validates NEXT_PUBLIC_MOBILE_APP_URL strictly and rejects empty or javascript: schemes', () => {
    delete process.env.NEXT_PUBLIC_MOBILE_APP_URL;
    expect(getMobileAppDownloadUrl()).toBeNull();

    process.env.NEXT_PUBLIC_MOBILE_APP_URL = '   ';
    expect(getMobileAppDownloadUrl()).toBeNull();

    process.env.NEXT_PUBLIC_MOBILE_APP_URL = 'javascript:alert(1)';
    expect(getMobileAppDownloadUrl()).toBeNull();

    process.env.NEXT_PUBLIC_MOBILE_APP_URL = ' https://example.com/eiu-pdds-v1.apk ';
    expect(getMobileAppDownloadUrl()).toBe('https://example.com/eiu-pdds-v1.apk');

    process.env.NEXT_PUBLIC_MOBILE_APP_URL = '/downloads/eiu-pdds.apk';
    expect(getMobileAppDownloadUrl()).toBe('/downloads/eiu-pdds.apk');
  });

  it('renders APK download button and AntD SVG QR in Header drawer when URL is set, and safe fallback when unset', async () => {
    process.env.NEXT_PUBLIC_MOBILE_APP_URL = 'https://example.com/eiu-pdds.apk';
    const scrollToSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});

    const { rerender } = render(
      <Providers>
        <SeedhouseHeader />
      </Providers>
    );

    act(() => {
      openMobileAppDrawer();
    });
    const apkLink = await screen.findByRole('link', { name: /tải apk android/i });
    expect(apkLink).toHaveAttribute('href', 'https://example.com/eiu-pdds.apk');
    expect(screen.getByTestId('mobile-app-qr')).toBeInTheDocument();
    expect(scrollToSpy).toHaveBeenCalled();

    delete process.env.NEXT_PUBLIC_MOBILE_APP_URL;
    rerender(
      <Providers>
        <SeedhouseHeader key="unset" />
      </Providers>
    );
    act(() => {
      openMobileAppDrawer();
    });
    expect(await screen.findByText(/bản cài android nội bộ/i)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /tải apk android/i })).not.toBeInTheDocument();
  });

  it('renders contextual mobile app triggers in PrescriptionSpread and HTTP 429 recovery stage', async () => {
    process.env.NEXT_PUBLIC_MOBILE_APP_URL = 'https://example.com/eiu-pdds.apk';
    const user = userEvent.setup();
    vi.spyOn(window, 'scrollTo').mockImplementation(() => {});

    const err429 = new PddsApiError(429, 'RATE_001', 'Too many guest uploads', 1800, {
      limit: 3,
      remaining: 0,
      retryAfterSeconds: 1800,
    });

    render(
      <Providers>
        <SeedhouseHeader />
        <LeafSpecimenStage
          diagnosis={SAMPLE_SPECIMENS[0].response}
          isSample={true}
          isLoading={false}
          error={err429}
          onRetryUpload={vi.fn()}
          onSelectSample={vi.fn()}
        />
        <PrescriptionSpread diagnosis={SAMPLE_SPECIMENS[0].response} />
      </Providers>
    );

    const openFromSpreadBtn = screen.getByRole('button', { name: /tải ứng dụng android/i });
    await user.click(openFromSpreadBtn);
    expect(await screen.findByTestId('mobile-app-qr')).toBeInTheDocument();

    await user.keyboard('{Escape}');
    await waitFor(() => {
      expect(screen.queryByTestId('mobile-app-qr')).not.toBeInTheDocument();
    });

    const openFrom429Btn = screen.getByRole('button', { name: /cài app quét không giới hạn/i });
    fireEvent.click(openFrom429Btn);
    expect(await screen.findByTestId('mobile-app-qr')).toBeInTheDocument();
  });
});
