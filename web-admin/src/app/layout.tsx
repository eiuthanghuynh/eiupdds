import type { Metadata } from 'next';
import './globals.css';
import { Sidebar } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';

export const metadata: Metadata = {
  title: 'EIU PDDS — Cổng Quản Trị Hệ Thống Chẩn Đoán Bệnh Cây Trồng',
  description: 'Trang quản trị cho chuyên gia nông nghiệp và phân tích phác đồ điều trị',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi">
      <body className="min-h-screen bg-slate-50 text-slate-900 antialiased">
        <Sidebar />
        <div className="pl-64">
          <Header />
          <main className="p-8">{children}</main>
        </div>
      </body>
    </html>
  );
}
