import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { Providers } from '@/app/providers';
import { SeedhouseHeader } from '@/components/layout/SeedhouseHeader';
import { useLanguage } from '@/lib/i18n/LanguageContext';

function Probe() {
  const { localizePlantName, localizeDiseaseName, localizeSeverity } = useLanguage();
  return (
    <div>
      <span data-testid="plant">{localizePlantName('POTATO', 'Potato')}</span>
      <span data-testid="disease">{localizeDiseaseName('POTATO_LATE_BLIGHT', 'Potato late blight')}</span>
      <span data-testid="sev">{localizeSeverity('CRITICAL')}</span>
    </div>
  );
}

describe('SeedhouseHeader & Bilingual Agronomic Dictionary', () => {
  it('defaults to farmer-friendly Vietnamese and switches cleanly to English', async () => {
    const user = userEvent.setup();
    render(
      <Providers>
        <SeedhouseHeader />
        <Probe />
      </Providers>
    );
    expect(screen.getByText('EIU PDDS')).toBeInTheDocument();
    expect(screen.getByTestId('plant')).toHaveTextContent('Khoai tây');
    expect(screen.getByTestId('disease')).toHaveTextContent('Bệnh mốc sương khoai tây');
    expect(screen.getByTestId('sev')).toHaveTextContent('Nguy hiểm khẩn cấp');

    await user.click(screen.getByRole('button', { name: /chuyển sang tiếng anh|english|en/i }));
    expect(screen.getByTestId('plant')).toHaveTextContent('Potato');
    expect(screen.getByTestId('disease')).toHaveTextContent('Potato late blight');
  });

  it('toggles inline Mobile App companion sheet without opening a modal or dead link', async () => {
    const user = userEvent.setup();
    render(
      <Providers>
        <SeedhouseHeader />
      </Providers>
    );
    await user.click(screen.getByRole('button', { name: /ứng dụng/i }));
    expect(screen.getByText(/lưu nhật ký đồng ruộng/i)).toBeInTheDocument();

    await user.keyboard('{Escape}');
    expect(screen.queryByText(/lưu nhật ký đồng ruộng/i)).not.toBeInTheDocument();

    const nav = screen.getByRole('navigation', { name: /điều hướng trang chủ/i });
    expect(nav.className).not.toMatch(/\bhidden\b/);
    expect(screen.getByRole('link', { name: /soi lá bệnh/i })).toHaveAttribute('href', '#workbench');
    expect(screen.queryByRole('link', { name: /cách hoạt động/i })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /sổ tay bệnh/i })).toHaveAttribute(
      'href',
      '#almanac'
    );
    expect(screen.getByRole('button', { name: /ứng dụng/i }).className).toMatch(/min-h-\[44px\]/);
  });
});
