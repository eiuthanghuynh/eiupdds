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
  Phone,
  ShieldCheck,
  User,
  UserPlus,
} from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { TitleComponent } from '@/components/common/TitleComponent';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { MaxContentWidth, Spacing } from '@/constants/theme';
import { useLanguage } from '@/context/LanguageContext';
import { useAppTheme } from '@/context/ThemeContext';

export default function RegisterScreen() {
  const { t } = useLanguage();
  const { colors, isDark } = useAppTheme();

  // Form states
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [agreedToTerms, setAgreedToTerms] = useState(false);

  // Interaction & UI feedback states
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [focusedInput, setFocusedInput] = useState<
    'fullName' | 'email' | 'phone' | 'password' | 'confirmPassword' | null
  >(null);

  // Calculate password strength indicator
  const getPasswordStrength = () => {
    if (!password) return { level: 0, label: '', color: 'transparent' };
    if (password.length < 6) return { level: 1, label: 'Yếu (ít hơn 6 ký tự)', color: isDark ? '#EF5350' : '#DC2626' };
    const hasNumber = /\d/.test(password);
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);
    if (password.length >= 8 && hasNumber && hasSpecial) {
      return { level: 3, label: 'Mạnh (bảo mật tối ưu)', color: isDark ? '#66BB6A' : '#2E7D32' };
    }
    return { level: 2, label: 'Trung bình', color: isDark ? '#FBBF24' : '#D97706' };
  };

  const strength = getPasswordStrength();

  const handleRegister = async () => {
    setErrorMessage(null);
    setSuccessMessage(null);

    // 1. Mandatory fields validation
    if (!fullName.trim() || !email.trim() || !phone.trim() || !password || !confirmPassword) {
      setErrorMessage(t('pleaseFillAllFields'));
      return;
    }

    // 2. Email format check
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) {
      setErrorMessage('Định dạng email không hợp lệ. Vui lòng kiểm tra lại.');
      return;
    }

    // 3. Password length check
    if (password.length < 6) {
      setErrorMessage('Mật khẩu phải chứa tối thiểu 6 ký tự.');
      return;
    }

    // 4. Password match check
    if (password !== confirmPassword) {
      setErrorMessage(t('passwordMismatch'));
      return;
    }

    // 5. Terms check
    if (!agreedToTerms) {
      setErrorMessage(t('mustAgreeTerms'));
      return;
    }

    try {
      setIsLoading(true);
      // Simulate account registration API call
      await new Promise((resolve) => setTimeout(resolve, 1200));

      setSuccessMessage(t('registerSuccess'));
      setTimeout(() => {
        router.replace('/login' as any);
      }, 900);
    } catch (err: any) {
      setErrorMessage(err?.message || 'Đăng ký không thành công. Vui lòng thử lại sau.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        {/* Fixed Header Title - Outside ScrollView per AGENTS.md */}
        <View style={styles.headerContainer}>
          <View style={styles.topNavRow}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Quay lại"
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
                  router.replace('/login' as any);
                }
              }}>
              <ArrowLeft size={20} color={colors.text} />
            </Pressable>

            <View style={styles.headerTitleWrap}>
              <TitleComponent
                title={t('registerTitle')}
                subtitle={t('registerSubtitle')}
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

            {/* Main Form Inputs Card */}
            <View
              style={[
                styles.formCard,
                {
                  backgroundColor: isDark ? '#1C2410' : '#E7EBD9',
                  borderColor: colors.border,
                },
              ]}>
              {/* 1. Full Name */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  {t('fullName')}
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        focusedInput === 'fullName'
                          ? isDark
                            ? '#CFFF74'
                            : '#2F3A10'
                          : colors.border,
                    },
                  ]}>
                  <User
                    size={20}
                    color={
                      focusedInput === 'fullName'
                        ? isDark
                          ? '#CFFF74'
                          : '#2F3A10'
                        : colors.textSecondary
                    }
                  />
                  <TextInput
                    style={[styles.textInput, { color: colors.text }]}
                    placeholder={t('fullNamePlaceholder')}
                    placeholderTextColor={colors.textSecondary}
                    value={fullName}
                    onChangeText={(val) => {
                      setFullName(val);
                      if (errorMessage) setErrorMessage(null);
                    }}
                    onFocus={() => setFocusedInput('fullName')}
                    onBlur={() => setFocusedInput(null)}
                    editable={!isLoading}
                  />
                </View>
              </View>

              {/* 2. Email */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  Email
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        focusedInput === 'email'
                          ? isDark
                            ? '#CFFF74'
                            : '#2F3A10'
                          : colors.border,
                    },
                  ]}>
                  <Mail
                    size={20}
                    color={
                      focusedInput === 'email'
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
                    value={email}
                    onChangeText={(val) => {
                      setEmail(val);
                      if (errorMessage) setErrorMessage(null);
                    }}
                    autoCapitalize="none"
                    keyboardType="email-address"
                    onFocus={() => setFocusedInput('email')}
                    onBlur={() => setFocusedInput(null)}
                    editable={!isLoading}
                  />
                </View>
              </View>

              {/* 3. Phone Number */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  {t('phoneNumber')}
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        focusedInput === 'phone'
                          ? isDark
                            ? '#CFFF74'
                            : '#2F3A10'
                          : colors.border,
                    },
                  ]}>
                  <Phone
                    size={20}
                    color={
                      focusedInput === 'phone'
                        ? isDark
                          ? '#CFFF74'
                          : '#2F3A10'
                        : colors.textSecondary
                    }
                  />
                  <TextInput
                    style={[styles.textInput, { color: colors.text }]}
                    placeholder={t('phoneNumberPlaceholder')}
                    placeholderTextColor={colors.textSecondary}
                    value={phone}
                    onChangeText={(val) => {
                      setPhone(val);
                      if (errorMessage) setErrorMessage(null);
                    }}
                    keyboardType="phone-pad"
                    onFocus={() => setFocusedInput('phone')}
                    onBlur={() => setFocusedInput(null)}
                    editable={!isLoading}
                  />
                </View>
              </View>

              {/* 4. Password with Strength Bar */}
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

                {/* Password requirement hint */}
                <ThemedText style={[styles.fieldRequirementText, { color: colors.textSecondary }]}>
                  Tối thiểu 6 ký tự, gồm số và ký tự đặc biệt để bảo mật tốt hơn
                </ThemedText>

                {/* Password Strength Meter */}
                {password.length > 0 && (
                  <View style={styles.strengthRow}>
                    <View style={styles.strengthBars}>
                      <View
                        style={[
                          styles.strengthBarSegment,
                          {
                            backgroundColor:
                              strength.level >= 1 ? strength.color : colors.border,
                          },
                        ]}
                      />
                      <View
                        style={[
                          styles.strengthBarSegment,
                          {
                            backgroundColor:
                              strength.level >= 2 ? strength.color : colors.border,
                          },
                        ]}
                      />
                      <View
                        style={[
                          styles.strengthBarSegment,
                          {
                            backgroundColor:
                              strength.level >= 3 ? strength.color : colors.border,
                          },
                        ]}
                      />
                    </View>
                    <ThemedText style={[styles.strengthLabel, { color: strength.color }]}>
                      {strength.label}
                    </ThemedText>
                  </View>
                )}
              </View>

              {/* 5. Confirm Password */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  {t('confirmPassword')}
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        focusedInput === 'confirmPassword'
                          ? isDark
                            ? '#CFFF74'
                            : '#2F3A10'
                          : colors.border,
                    },
                  ]}>
                  <ShieldCheck
                    size={20}
                    color={
                      focusedInput === 'confirmPassword'
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
                    value={confirmPassword}
                    onChangeText={(val) => {
                      setConfirmPassword(val);
                      if (errorMessage) setErrorMessage(null);
                    }}
                    secureTextEntry={!showConfirmPassword}
                    onFocus={() => setFocusedInput('confirmPassword')}
                    onBlur={() => setFocusedInput(null)}
                    editable={!isLoading}
                  />
                  <Pressable
                    accessibilityRole="button"
                    style={styles.eyeButton}
                    hitSlop={8}
                    onPress={() => setShowConfirmPassword(!showConfirmPassword)}>
                    {showConfirmPassword ? (
                      <EyeOff size={20} color={colors.textSecondary} />
                    ) : (
                      <Eye size={20} color={colors.textSecondary} />
                    )}
                  </Pressable>
                </View>
              </View>

              {/* 6. Terms & Conditions Agreement */}
              <Pressable
                accessibilityRole="checkbox"
                accessibilityState={{ checked: agreedToTerms }}
                style={styles.termsAgreementRow}
                onPress={() => setAgreedToTerms(!agreedToTerms)}>
                <View
                  style={[
                    styles.checkboxBox,
                    {
                      backgroundColor: agreedToTerms
                        ? isDark
                          ? '#CFFF74'
                          : '#2F3A10'
                        : 'transparent',
                      borderColor: isDark ? '#CFFF74' : '#2F3A10',
                    },
                  ]}>
                  {agreedToTerms && (
                    <CheckCircle2
                      size={14}
                      color={isDark ? '#111609' : '#ffffff'}
                    />
                  )}
                </View>
                <ThemedText style={[styles.termsText, { color: colors.text }]}>
                  {t('termsAgreement')}
                </ThemedText>
              </Pressable>

              {/* Primary CTA Button: Lime Neon Background with Dark Moss Text */}
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={t('signUpBtn')}
                disabled={isLoading}
                style={({ pressed }) => [
                  styles.primaryButton,
                  { backgroundColor: '#CFFF74' },
                  pressed && styles.buttonPressed,
                  isLoading && styles.buttonDisabled,
                ]}
                onPress={handleRegister}>
                {isLoading ? (
                  <ActivityIndicator size="small" color="#2F3A10" />
                ) : (
                  <>
                    <UserPlus size={20} color="#2F3A10" />
                    <ThemedText style={styles.primaryButtonText}>
                      {t('signUpBtn')}
                    </ThemedText>
                  </>
                )}
              </Pressable>
            </View>
          </ScrollView>

          {/* Fixed Footer: Switch to Login Screen pinned at bottom */}
          <View
            style={[
              styles.fixedFooter,
              {
                backgroundColor: isDark ? '#111609' : '#F7F8F1',
                borderTopColor: colors.border,
              },
            ]}>
            <ThemedText style={[styles.footerPrompt, { color: colors.textSecondary }]}>
              {t('alreadyHaveAccount')}
            </ThemedText>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={t('loginNow')}
              hitSlop={8}
              style={({ pressed }) => [styles.linkButton, pressed && styles.pressed]}
              onPress={() => router.replace('/login' as any)}>
              <ThemedText
                style={[
                  styles.switchLinkText,
                  { color: isDark ? '#CFFF74' : '#2F3A10' },
                ]}>
                {t('loginNow')}
              </ThemedText>
            </Pressable>
          </View>
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
    paddingTop: Spacing.two,
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
  fieldRequirementText: {
    fontSize: 12,
    fontWeight: '500',
    lineHeight: 16,
    marginTop: 2,
    paddingHorizontal: 2,
  },
  strengthRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 4,
  },
  strengthBars: {
    flexDirection: 'row',
    gap: 4,
    width: 90,
  },
  strengthBarSegment: {
    flex: 1,
    height: 4,
    borderRadius: 2,
  },
  strengthLabel: {
    fontSize: 12,
    fontWeight: '600',
  },
  termsAgreementRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.two,
    marginTop: Spacing.one,
  },
  checkboxBox: {
    width: 22,
    height: 22,
    borderRadius: 6,
    borderWidth: 1.5,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 2,
  },
  termsText: {
    flex: 1,
    fontSize: 13,
    fontWeight: '500',
    lineHeight: 18,
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
  fixedFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.one,
    paddingVertical: Spacing.three,
    paddingHorizontal: Spacing.four,
    borderTopWidth: 1,
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
