'use client';

import React, { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import { LazyMotion, MotionConfig, domMax } from 'motion/react';
import { seedhouseAntdTheme } from '@/lib/theme/seedhouseTheme';
import { LanguageProvider } from '@/lib/i18n/LanguageContext';

export function Providers({ children }: { children: React.ReactNode }): React.JSX.Element {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60_000,
            refetchOnWindowFocus: false,
            retry: 1,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider theme={seedhouseAntdTheme}>
        <LazyMotion features={domMax}>
          <MotionConfig reducedMotion="user">
            <LanguageProvider>{children}</LanguageProvider>
          </MotionConfig>
        </LazyMotion>
      </ConfigProvider>
    </QueryClientProvider>
  );
}
