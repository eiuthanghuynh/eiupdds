import React, { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  TextInput,
  View,
} from 'react-native';
import { router } from 'expo-router';
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Eye,
  EyeOff,
  Lock,
  Mail,
  ShieldCheck,
} from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { TitleComponent } from '@/components/common/TitleComponent';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { MaxContentWidth, Spacing } from '@/constants/theme';
import { useAuth } from '@/context/AuthContext';
import { useLanguage } from '@/context/LanguageContext';
import { useAppTheme } from '@/context/ThemeContext';

export default function LoginScreen() {
  const { t } = useLanguage();
  const { colors, isDark } = useAppTheme();
  const { login } = useAuth();

  // Form states
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);

  // Interaction & UI states (Loading, Error, Success)
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [focusedInput, setFocusedInput] = useState<'identifier' | 'password' | null>(null);

  const handleSignIn = async () => {
    // Clear previous feedback
    setErrorMessage(null);
    setSuccessMessage(null);

    // Validation
    if (!identifier.trim() || !password.trim()) {
      setErrorMessage(t('pleaseFillAllFields'));
      return;
    }

    if (password.length < 6) {
      setErrorMessage(t('passwordCriteriaShort'));
      return;
    }

    try {
      setIsLoading(true);
      await login({
        emailOrUsername: identifier.trim(),
        password: password.trim(),
        deviceInfo: Platform.OS === 'web' ? 'Web Client' : `${Platform.OS} Mobile Client`,
      });

      setSuccessMessage(t('loginSuccess'));
      setTimeout(() => {
        if (router.canGoBack()) {
          router.back();
        } else {
          router.replace('/(tabs)' as any);
        }
      }, 700);
    } catch (err: any) {
      const serverMessage = err?.message || t('loginFailed');
      const serverCode = err?.code ? ` [${err.code}]` : '';
      setErrorMessage(`${serverMessage}${serverCode}`);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        {/* Fixed Header Title - Strictly outside ScrollView per AGENTS.md */}
        <View style={styles.headerContainer}>
          <View style={styles.topNavRow}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={t('back')}
              style={({ pressed }) => [
                styles.backButton,
                {
                  backgroundColor: isDark ? '#1C2410' : '#E7EBD9',
                  borderColor: colors.border,
                },
                pressed && styles.pressed,
              ]}
              onPress={() => {
                if (router.canGoBack()) {
                  router.back();
                } else {
                  router.replace('/(tabs)' as any);
                }
              }}>
              <ArrowLeft size={20} color={colors.text} />
            </Pressable>

            <View style={styles.headerTitleWrap}>
              <TitleComponent
                title={t('loginTitle')}
                subtitle={t('loginSubtitle')}
              />
            </View>
          </View>
        </View>

        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
          style={styles.keyboardView}>
          <ScrollView
            style={styles.scrollView}
            contentContainerStyle={styles.scrollContent}
            showsVerticalScrollIndicator={false}
            keyboardShouldPersistTaps="handled">

            {/* Error Feedback Banner */}
            {errorMessage && (
              <View
                style={[
                  styles.alertBanner,
                  {
                    backgroundColor: isDark ? 'rgba(239, 83, 80, 0.15)' : '#FEE2E2',
                    borderColor: isDark ? '#EF5350' : '#DC2626',
                  },
                ]}>
                <AlertCircle size={20} color={isDark ? '#EF5350' : '#DC2626'} />
                <ThemedText
                  style={[styles.alertText, { color: isDark ? '#EF5350' : '#DC2626' }]}>
                  {errorMessage}
                </ThemedText>
              </View>
            )}

            {/* Success Feedback Banner */}
            {successMessage && (
              <View
                style={[
                  styles.alertBanner,
                  {
                    backgroundColor: isDark ? 'rgba(102, 187, 106, 0.15)' : '#DCFCE7',
                    borderColor: isDark ? '#66BB6A' : '#15803D',
                  },
                ]}>
                <CheckCircle2 size={20} color={isDark ? '#66BB6A' : '#15803D'} />
                <ThemedText
                  style={[styles.alertText, { color: isDark ? '#66BB6A' : '#15803D' }]}>
                  {successMessage}
                </ThemedText>
              </View>
            )}

            {/* Main Input Form Card */}
            <View
              style={[
                styles.formCard,
                {
                  backgroundColor: isDark ? '#1C2410' : '#E7EBD9',
                  borderColor: colors.border,
                },
              ]}>
              {/* Field 1: Email or Username */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  {t('emailOrUsername')}
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        focusedInput === 'identifier'
                          ? isDark
                            ? '#CFFF74'
                            : '#2F3A10'
                          : colors.border,
                    },
                  ]}>
                  <Mail
                    size={20}
                    color={
                      focusedInput === 'identifier'
                        ? isDark
                          ? '#CFFF74'
                          : '#2F3A10'
                        : colors.textSecondary
                    }
                  />
                  <TextInput
                    style={[styles.textInput, { color: colors.text }]}
                    placeholder={t('emailPlaceholder')}
                    placeholderTextColor={colors.textSecondary}
                    value={identifier}
                    onChangeText={(val) => {
                      setIdentifier(val);
                      if (errorMessage) setErrorMessage(null);
                    }}
                    autoCapitalize="none"
                    keyboardType="email-address"
                    onFocus={() => setFocusedInput('identifier')}
                    onBlur={() => setFocusedInput(null)}
                    editable={!isLoading}
                  />
                </View>
              </View>

              {/* Field 2: Password */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  {t('password')}
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        focusedInput === 'password'
                          ? isDark
                            ? '#CFFF74'
                            : '#2F3A10'
                          : colors.border,
                    },
                  ]}>
                  <Lock
                    size={20}
                    color={
                      focusedInput === 'password'
                        ? isDark
                          ? '#CFFF74'
                          : '#2F3A10'
                        : colors.textSecondary
                    }
                  />
                  <TextInput
                    style={[styles.textInput, { color: colors.text }]}
                    placeholder="••••••••"
                    placeholderTextColor={colors.textSecondary}
                    value={password}
                    onChangeText={(val) => {
                      setPassword(val);
                      if (errorMessage) setErrorMessage(null);
                    }}
                    secureTextEntry={!showPassword}
                    onFocus={() => setFocusedInput('password')}
                    onBlur={() => setFocusedInput(null)}
                    editable={!isLoading}
                  />
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    style={styles.eyeButton}
                    hitSlop={8}
                    onPress={() => setShowPassword(!showPassword)}>
                    {showPassword ? (
                      <EyeOff size={20} color={colors.textSecondary} />
                    ) : (
                      <Eye size={20} color={colors.textSecondary} />
                    )}
                  </Pressable>
                </View>
              </View>

              {/* Options Row: Remember Me & Forgot Password horizontally aligned */}
              <View style={styles.optionsRow}>
                <Pressable
                  accessibilityRole="checkbox"
                  accessibilityState={{ checked: rememberMe }}
                  style={({ pressed }) => [styles.rememberRow, pressed && styles.pressed]}
                  onPress={() => setRememberMe(!rememberMe)}>
                  <View
                    style={[
                      styles.checkboxBox,
                      {
                        backgroundColor: rememberMe ? (isDark ? '#CFFF74' : '#2F3A10') : 'transparent',
                        borderColor: isDark ? '#CFFF74' : '#2F3A10',
                      },
                    ]}>
                    {rememberMe && (
                      <CheckCircle2
                        size={14}
                        color={isDark ? '#111609' : '#ffffff'}
                      />
                    )}
                  </View>
                  <ThemedText style={[styles.rememberText, { color: colors.text }]}>
                    {t('rememberMe')}
                  </ThemedText>
                </Pressable>

                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={t('forgotPassword')}
                  hitSlop={8}
                  style={({ pressed }) => [styles.forgotButton, pressed && styles.pressed]}
                  onPress={() => {
                    setErrorMessage(t('forgotPasswordNotice'));
                  }}>
                  <ThemedText style={[styles.forgotLink, { color: isDark ? '#CFFF74' : '#2F3A10' }]}>
                    {t('forgotPassword')}
                  </ThemedText>
                </Pressable>
              </View>

              {/* Primary CTA Button - Lime Neon Background with Dark Moss Text per .impeccable.md */}
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={t('signInBtn')}
                disabled={isLoading}
                style={({ pressed }) => [
                  styles.primaryButton,
                  { backgroundColor: '#CFFF74' },
                  pressed && styles.buttonPressed,
                  isLoading && styles.buttonDisabled,
                ]}
                onPress={handleSignIn}>
                {isLoading ? (
                  <ActivityIndicator size="small" color="#2F3A10" />
                ) : (
                  <>
                    <ShieldCheck size={20} color="#2F3A10" />
                    <ThemedText style={styles.primaryButtonText}>
                      {t('signInBtn')}
                    </ThemedText>
                  </>
                )}
              </Pressable>
            </View>

            {/* Switch to Register Screen */}
            <View style={styles.footerSection}>
              <ThemedText style={[styles.footerPrompt, { color: colors.textSecondary }]}>
                {t('noAccountYet')}
              </ThemedText>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={t('registerNow')}
                style={({ pressed }) => [styles.linkButton, pressed && styles.pressed]}
                onPress={() => router.push('/register' as any)}>
                <ThemedText
                  style={[
                    styles.switchLinkText,
                    { color: isDark ? '#CFFF74' : '#2F3A10' },
                  ]}>
                  {t('registerNow')}
                </ThemedText>
              </Pressable>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    flexDirection: 'row',
  },
  safeArea: {
    flex: 1,
    maxWidth: MaxContentWidth,
    width: '100%',
  },
  headerContainer: {
    width: '100%',
    paddingHorizontal: Spacing.four,
    paddingTop: Spacing.two,
    paddingBottom: Spacing.two,
    zIndex: 10,
  },
  topNavRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
  },
  backButton: {
    width: 44,
    height: 44,
    borderRadius: 12,
    borderWidth: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitleWrap: {
    flex: 1,
  },
  keyboardView: {
    flex: 1,
  },
  scrollView: {
    flex: 1,
    width: '100%',
  },
  scrollContent: {
    paddingHorizontal: Spacing.four,
    paddingTop: Spacing.four,
    paddingBottom: Spacing.six,
  },
  alertBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    paddingVertical: 12,
    paddingHorizontal: Spacing.three,
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: Spacing.three,
  },
  alertText: {
    flex: 1,
    fontSize: 13,
    fontWeight: '600',
    lineHeight: 18,
  },
  formCard: {
    borderRadius: 20,
    padding: Spacing.four,
    borderWidth: 1,
    gap: Spacing.three,
  },
  fieldGroup: {
    gap: Spacing.one,
  },
  fieldLabel: {
    fontSize: 14,
    fontWeight: '700',
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 52,
    borderRadius: 14,
    borderWidth: 1.5,
    paddingHorizontal: Spacing.three,
    gap: Spacing.two,
  },
  textInput: {
    flex: 1,
    height: '100%',
    fontSize: 15,
    fontWeight: '500',
  },
  eyeButton: {
    width: 44,
    height: 44,
    justifyContent: 'center',
    alignItems: 'center',
  },
  optionsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.one,
  },
  rememberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    minHeight: 44,
  },
  forgotButton: {
    justifyContent: 'center',
    alignItems: 'flex-end',
    minHeight: 44,
    paddingVertical: 4,
  },
  forgotLink: {
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: -0.2,
    textDecorationLine: 'underline',
  },
  checkboxBox: {
    width: 22,
    height: 22,
    borderRadius: 6,
    borderWidth: 1.5,
    justifyContent: 'center',
    alignItems: 'center',
  },
  rememberText: {
    fontSize: 14,
    fontWeight: '600',
  },
  primaryButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    height: 52,
    borderRadius: 14,
    gap: Spacing.two,
    marginTop: Spacing.two,
    elevation: 2,
    shadowColor: '#2F3A10',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.12,
    shadowRadius: 5,
  },
  primaryButtonText: {
    color: '#2F3A10', // Strictly Dark Moss on Lime Neon per .impeccable.md
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 0.3,
  },
  footerSection: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.one,
    marginTop: Spacing.four,
    flexWrap: 'wrap',
  },
  footerPrompt: {
    fontSize: 14,
    fontWeight: '500',
  },
  linkButton: {
    paddingVertical: 4,
    paddingHorizontal: 4,
  },
  switchLinkText: {
    fontSize: 14,
    fontWeight: '800',
    textDecorationLine: 'underline',
  },
  pressed: {
    opacity: 0.75,
  },
  buttonPressed: {
    opacity: 0.88,
    transform: [{ scale: 0.99 }],
  },
  buttonDisabled: {
    opacity: 0.65,
  },
});
