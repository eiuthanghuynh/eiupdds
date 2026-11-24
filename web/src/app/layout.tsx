import type { Metadata } from 'next';
import { Be_Vietnam_Pro, Literata } from 'next/font/google';
import { AntdRegistry } from '@ant-design/nextjs-registry';
import { Providers } from './providers';
import './globals.css';

const literata = Literata({
  subsets: ['latin', 'vietnamese'],
  weight: ['600', '700'],
  variable: '--font-display',
  display: 'swap',
});

const beVietnamPro = Be_Vietnam_Pro({
  subsets: ['latin', 'vietnamese'],
  weight: ['400', '500', '600', '700'],
  variable: '--font-body',
  display: 'swap',
});

export const metadata: Metadata = {
  title: 'EIU PDDS — Plant Disease Detection System',
  description:
    'EIU Plant Disease Detection System (EIU PDDS): Capture or upload leaf photos to inspect disease lesion bounding boxes and browse safe agronomic treatment protocols.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>): React.JSX.Element {
  return (
    <html lang="vi" className={`${literata.variable} ${beVietnamPro.variable}`}>
      <body className="min-h-screen flex flex-col bg-kraft text-ink font-body">
        <AntdRegistry>
          <Providers>{children}</Providers>
        </AntdRegistry>
      </body>
    </html>
  );
}
