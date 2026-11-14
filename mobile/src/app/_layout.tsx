import React from 'react';
import { DefaultTheme, ThemeProvider, Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';

import { AnimatedSplashOverlay } from '@/components/animated-icon';
import { GluestackUIProvider } from '@/components/ui/gluestack-ui-provider';
import { Colors } from '@/constants/theme';
import { AuthProvider } from '@/context/AuthContext';
import { DiagnosisProvider } from '@/context/DiagnosisContext';
import { LanguageProvider } from '@/context/LanguageContext';
import { AppThemeProvider, useAppTheme } from '@/context/ThemeContext';
import '@/global.css';

SplashScreen.preventAutoHideAsync();

const LightAppTheme = {
  ...DefaultTheme,
  dark: false,
  colors: {
    ...DefaultTheme.colors,
    background: Colors.light.background,
    card: Colors.light.surface,
    text: Colors.light.text,
    border: Colors.light.border,
    primary: Colors.light.primary,
  },
};

const DarkAppTheme = {
  ...DefaultTheme,
  dark: true,
  colors: {
    ...DefaultTheme.colors,
    background: Colors.dark.background,
    card: Colors.dark.surface,
    text: Colors.dark.text,
    border: Colors.dark.border,
    primary: Colors.dark.primary,
  },
};

function InnerLayout() {
  const { mode, isDark } = useAppTheme();
  const currentTheme = isDark ? DarkAppTheme : LightAppTheme;

  return (
    <GluestackUIProvider mode={mode}>
      <ThemeProvider value={currentTheme}>
        <StatusBar style={isDark ? 'light' : 'dark'} />
        <AnimatedSplashOverlay />
        <Stack screenOptions={{ headerShown: false }}>
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
          <Stack.Screen
            name="result"
            options={{
              headerShown: false,
              animation: 'slide_from_right',
            }}
          />
          <Stack.Screen
            name="login"
            options={{
              headerShown: false,
              animation: 'slide_from_bottom',
            }}
          />
          <Stack.Screen
            name="register"
            options={{
              headerShown: false,
              animation: 'slide_from_right',
            }}
          />
        </Stack>
      </ThemeProvider>
    </GluestackUIProvider>
  );
}

export default function RootLayout() {
  return (
    <AppThemeProvider>
      <LanguageProvider>
        <AuthProvider>
          <DiagnosisProvider>
            <InnerLayout />
          </DiagnosisProvider>
        </AuthProvider>
      </LanguageProvider>
    </AppThemeProvider>
  );
}
