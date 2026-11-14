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
  XCircle,
} from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { TitleComponent } from '@/components/common/TitleComponent';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { MaxContentWidth, Spacing } from '@/constants/theme';
import { useAuth } from '@/context/AuthContext';
import { useLanguage } from '@/context/LanguageContext';
import { useAppTheme } from '@/context/ThemeContext';

export default function RegisterScreen() {
  const { t } = useLanguage();
  const { colors, isDark } = useAppTheme();
  const { register } = useAuth();

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
  const [termsError, setTermsError] = useState(false);
  const [focusedInput, setFocusedInput] = useState<
    'fullName' | 'email' | 'phone' | 'password' | 'confirmPassword' | null
  >(null);
  const [touched, setTouched] = useState<{
    fullName?: boolean;
    email?: boolean;
    phone?: boolean;
    password?: boolean;
    confirmPassword?: boolean;
  }>({});

  // 1. Password validation criteria (Yêu cầu: số, chữ thường, chữ hoa, tối thiểu 6 ký tự)
  const hasMinLength = password.length >= 6;
  const hasLower = /[a-z]/.test(password);
  const hasUpper = /[A-Z]/.test(password);
  const hasNumber = /\d/.test(password);
  const isPasswordValid = hasMinLength && hasLower && hasUpper && hasNumber;

  // Password strength calculation
  const getPasswordStrength = () => {
    if (!password) return { level: 0, label: '', color: 'transparent' };
    if (!hasMinLength) {
      return { level: 1, label: t('pwdStrengthWeak'), color: isDark ? '#EF5350' : '#DC2626' };
    }
    if (!isPasswordValid) {
      return { level: 2, label: t('pwdStrengthModerate'), color: isDark ? '#FBBF24' : '#D97706' };
    }
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);
    if (password.length >= 8 && hasSpecial) {
      return { level: 3, label: t('pwdStrengthStrong'), color: isDark ? '#66BB6A' : '#2E7D32' };
    }
    return { level: 3, label: t('pwdStrengthStandard'), color: isDark ? '#66BB6A' : '#2E7D32' };
  };

  const strength = getPasswordStrength();

  // 2. Email format & empty validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const isEmailEmpty = !email.trim();
  const isEmailFormatValid = emailRegex.test(email.trim());
  const emailError =
    touched.email && (isEmailEmpty ? t('emailRequired') : !isEmailFormatValid ? t('emailInvalid') : null);

  // 3. Vietnam phone format validation (10 digits starting with 03, 05, 07, 08, 09 or +84)
  const cleanPhone = phone.trim().replace(/\s+/g, '');
  const isPhoneEmpty = !cleanPhone;
  const isPhoneVN = /^(0|\+?84)(3|5|7|8|9)[0-9]{8}$/.test(cleanPhone);
  const phoneError =
    touched.phone && (isPhoneEmpty ? t('phoneRequired') : !isPhoneVN ? t('phoneInvalid') : null);

  // 4. Password inline error
  const passwordError =
    touched.password &&
    (!password
      ? t('passwordRequired')
      : !isPasswordValid
        ? t('passwordCriteria')
        : null);

  // 5. Confirm password match validation
  const isConfirmMatch = password === confirmPassword;
  const confirmError =
    touched.confirmPassword && (!confirmPassword ? t('confirmPasswordRequired') : !isConfirmMatch ? t('confirmPasswordMismatch') : null);

  // 6. Full name validation
  const fullNameError = touched.fullName && !fullName.trim() ? t('fullNameRequired') : null;

  // Phone input handler: strictly numeric only (prevent alphabet letters)
  const handlePhoneChange = (val: string) => {
    const numericOnly = val.replace(/[^0-9+]/g, '');
    setPhone(numericOnly);
    if (errorMessage) setErrorMessage(null);
  };

  const handleRegister = async () => {
    setErrorMessage(null);
    setSuccessMessage(null);

    // Mark all fields touched to show inline validation if empty
    setTouched({
      fullName: true,
      email: true,
      phone: true,
      password: true,
      confirmPassword: true,
    });

    // 1. Mandatory fields check
    if (!fullName.trim() || isEmailEmpty || isPhoneEmpty || !password || !confirmPassword) {
      setErrorMessage(t('fillRequiredFields'));
      return;
    }

    // 2. Email format check
    if (!isEmailFormatValid) {
      setErrorMessage(t('emailInvalidShort'));
      return;
    }

    // 3. Vietnam phone format check
    if (!isPhoneVN) {
      setErrorMessage(t('phoneInvalidShort'));
      return;
    }

    // 4. Password requirement check (số, chữ thường, chữ hoa)
    if (!isPasswordValid) {
      setErrorMessage(t('passwordCriteriaShort'));
      return;
    }

    // 5. Password match check
    if (!isConfirmMatch) {
      setErrorMessage(t('passwordMismatch'));
      return;
    }

    // 6. Policy terms agreement check
    if (!agreedToTerms) {
      setTermsError(true);
      setErrorMessage(t('termsNotAgreed'));
      return;
    }

    // Derive unique username from email for backend requirement
    const emailPrefix = email.trim().split('@')[0].replace(/[^a-zA-Z0-9_]/g, '_').slice(0, 30);
    const username = emailPrefix.length >= 3 ? emailPrefix : `user_${Date.now().toString().slice(-6)}`;

    try {
      setIsLoading(true);
      await register({
        username,
        email: email.trim().toLowerCase(),
        password,
        fullName: fullName.trim(),
        phoneNumber: cleanPhone,
      });

      // API returned 201 Created: Show prominent success notification
      setSuccessMessage(t('regSuccess'));
      setTimeout(() => {
        router.replace('/login' as any);
      }, 1500);
    } catch {
      setErrorMessage(t('regFailed'));
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
                <XCircle size={20} color={isDark ? '#EF5350' : '#DC2626'} />
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
                  {t('fullName')} <ThemedText style={[styles.requiredStar, { color: isDark ? '#EF5350' : '#DC2626' }]}>*</ThemedText>
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        fullNameError
                          ? isDark ? '#EF5350' : '#DC2626'
                          : focusedInput === 'fullName'
                            ? isDark ? '#CFFF74' : '#2F3A10'
                            : colors.border,
                    },
                  ]}>
                  <User
                    size={20}
                    color={
                      fullNameError
                        ? isDark ? '#EF5350' : '#DC2626'
                        : focusedInput === 'fullName'
                          ? isDark ? '#CFFF74' : '#2F3A10'
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
                    onBlur={() => {
                      setFocusedInput(null);
                      setTouched((prev) => ({ ...prev, fullName: true }));
                    }}
                    editable={!isLoading}
                  />
                </View>
                {fullNameError && (
                  <ThemedText style={[styles.fieldInlineError, { color: isDark ? '#EF5350' : '#DC2626' }]}>
                    {fullNameError}
                  </ThemedText>
                )}
              </View>

              {/* 2. Email */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  Email <ThemedText style={[styles.requiredStar, { color: isDark ? '#EF5350' : '#DC2626' }]}>*</ThemedText>
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        emailError
                          ? isDark ? '#EF5350' : '#DC2626'
                          : focusedInput === 'email'
                            ? isDark ? '#CFFF74' : '#2F3A10'
                            : colors.border,
                    },
                  ]}>
                  <Mail
                    size={20}
                    color={
                      emailError
                        ? isDark ? '#EF5350' : '#DC2626'
                        : focusedInput === 'email'
                          ? isDark ? '#CFFF74' : '#2F3A10'
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
                    onBlur={() => {
                      setFocusedInput(null);
                      setTouched((prev) => ({ ...prev, email: true }));
                    }}
                    editable={!isLoading}
                  />
                </View>
                {emailError && (
                  <ThemedText style={[styles.fieldInlineError, { color: isDark ? '#EF5350' : '#DC2626' }]}>
                    {emailError}
                  </ThemedText>
                )}
              </View>

              {/* 3. Phone Number */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  {t('phoneNumber')} <ThemedText style={[styles.requiredStar, { color: isDark ? '#EF5350' : '#DC2626' }]}>*</ThemedText>
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        phoneError
                          ? isDark ? '#EF5350' : '#DC2626'
                          : focusedInput === 'phone'
                            ? isDark ? '#CFFF74' : '#2F3A10'
                            : colors.border,
                    },
                  ]}>
                  <Phone
                    size={20}
                    color={
                      phoneError
                        ? isDark ? '#EF5350' : '#DC2626'
                        : focusedInput === 'phone'
                          ? isDark ? '#CFFF74' : '#2F3A10'
                          : colors.textSecondary
                    }
                  />
                  <TextInput
                    style={[styles.textInput, { color: colors.text }]}
                    placeholder={t('phoneNumberPlaceholder')}
                    placeholderTextColor={colors.textSecondary}
                    value={phone}
                    onChangeText={handlePhoneChange}
                    keyboardType="phone-pad"
                    onFocus={() => setFocusedInput('phone')}
                    onBlur={() => {
                      setFocusedInput(null);
                      setTouched((prev) => ({ ...prev, phone: true }));
                    }}
                    editable={!isLoading}
                  />
                </View>
                {phoneError && (
                  <ThemedText style={[styles.fieldInlineError, { color: isDark ? '#EF5350' : '#DC2626' }]}>
                    {phoneError}
                  </ThemedText>
                )}
              </View>

              {/* 4. Password with Real-time Criteria & Strength Bar */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  {t('password')} <ThemedText style={[styles.requiredStar, { color: isDark ? '#EF5350' : '#DC2626' }]}>*</ThemedText>
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        passwordError
                          ? isDark ? '#EF5350' : '#DC2626'
                          : focusedInput === 'password'
                            ? isDark ? '#CFFF74' : '#2F3A10'
                            : colors.border,
                    },
                  ]}>
                  <Lock
                    size={20}
                    color={
                      passwordError
                        ? isDark ? '#EF5350' : '#DC2626'
                        : focusedInput === 'password'
                          ? isDark ? '#CFFF74' : '#2F3A10'
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
                    onBlur={() => {
                      setFocusedInput(null);
                      setTouched((prev) => ({ ...prev, password: true }));
                    }}
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

                {/* Real-time Password Criteria Checklist */}
                <View style={styles.criteriaContainer}>
                  <View style={styles.criterionItem}>
                    <CheckCircle2
                      size={14}
                      color={
                        hasMinLength
                          ? isDark ? '#66BB6A' : '#15803D'
                          : colors.textSecondary
                      }
                    />
                    <ThemedText
                      style={[
                        styles.criterionText,
                        {
                          color: hasMinLength
                            ? isDark ? '#66BB6A' : '#15803D'
                            : colors.textSecondary,
                        },
                      ]}>
                      {t('pwdCriterionMinLength')}
                    </ThemedText>
                  </View>
                  <View style={styles.criterionItem}>
                    <CheckCircle2
                      size={14}
                      color={
                        hasLower && hasUpper
                          ? isDark ? '#66BB6A' : '#15803D'
                          : colors.textSecondary
                      }
                    />
                    <ThemedText
                      style={[
                        styles.criterionText,
                        {
                          color: hasLower && hasUpper
                            ? isDark ? '#66BB6A' : '#15803D'
                            : colors.textSecondary,
                        },
                      ]}>
                      {t('pwdCriterionLowerUpper')}
                    </ThemedText>
                  </View>
                  <View style={styles.criterionItem}>
                    <CheckCircle2
                      size={14}
                      color={
                        hasNumber
                          ? isDark ? '#66BB6A' : '#15803D'
                          : colors.textSecondary
                      }
                    />
                    <ThemedText
                      style={[
                        styles.criterionText,
                        {
                          color: hasNumber
                            ? isDark ? '#66BB6A' : '#15803D'
                            : colors.textSecondary,
                        },
                      ]}>
                      {t('pwdCriterionNumber')}
                    </ThemedText>
                  </View>
                </View>

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

                {passwordError && (
                  <ThemedText style={[styles.fieldInlineError, { color: isDark ? '#EF5350' : '#DC2626' }]}>
                    {passwordError}
                  </ThemedText>
                )}
              </View>

              {/* 5. Confirm Password */}
              <View style={styles.fieldGroup}>
                <ThemedText style={[styles.fieldLabel, { color: colors.text }]}>
                  {t('confirmPassword')} <ThemedText style={[styles.requiredStar, { color: isDark ? '#EF5350' : '#DC2626' }]}>*</ThemedText>
                </ThemedText>
                <View
                  style={[
                    styles.inputContainer,
                    {
                      backgroundColor: isDark ? '#111609' : '#F7F8F1',
                      borderColor:
                        confirmError
                          ? isDark ? '#EF5350' : '#DC2626'
                          : focusedInput === 'confirmPassword'
                            ? isDark ? '#CFFF74' : '#2F3A10'
                            : colors.border,
                    },
                  ]}>
                  <ShieldCheck
                    size={20}
                    color={
                      confirmError
                        ? isDark ? '#EF5350' : '#DC2626'
                        : focusedInput === 'confirmPassword'
                          ? isDark ? '#CFFF74' : '#2F3A10'
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
                    onBlur={() => {
                      setFocusedInput(null);
                      setTouched((prev) => ({ ...prev, confirmPassword: true }));
                    }}
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
                {confirmError && (
                  <ThemedText style={[styles.fieldInlineError, { color: isDark ? '#EF5350' : '#DC2626' }]}>
                    {confirmError}
                  </ThemedText>
                )}
              </View>

              {/* 6. Terms & Conditions Agreement */}
              <View
                style={[
                  styles.termsWrapper,
                  termsError && {
                    backgroundColor: isDark ? 'rgba(239, 83, 80, 0.12)' : '#FEF2F2',
                    borderColor: isDark ? '#EF5350' : '#DC2626',
                    borderWidth: 1,
                  },
                ]}>
                <Pressable
                  accessibilityRole="checkbox"
                  accessibilityState={{ checked: agreedToTerms }}
                  style={styles.termsAgreementRow}
                  onPress={() => {
                    const nextVal = !agreedToTerms;
                    setAgreedToTerms(nextVal);
                    if (nextVal) {
                      setTermsError(false);
                    }
                    if (errorMessage) setErrorMessage(null);
                  }}>
                  <View
                    style={[
                      styles.checkboxBox,
                      {
                        backgroundColor: agreedToTerms
                          ? isDark
                            ? '#CFFF74'
                            : '#2F3A10'
                          : 'transparent',
                        borderColor: termsError
                          ? isDark
                            ? '#EF5350'
                            : '#DC2626'
                          : isDark
                          ? '#CFFF74'
                          : '#2F3A10',
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
                    {t('termsAgreement')} <ThemedText style={[styles.requiredStar, { color: isDark ? '#EF5350' : '#DC2626' }]}>*</ThemedText>
                  </ThemedText>
                </Pressable>

                {termsError && (
                  <View style={styles.termsWarningBox}>
                    <XCircle size={14} color={isDark ? '#EF5350' : '#DC2626'} />
                    <ThemedText
                      style={[
                        styles.termsInlineError,
                        { color: isDark ? '#EF5350' : '#DC2626' },
                      ]}>
                      {t('termsNotAgreed')}
                    </ThemedText>
                  </View>
                )}
              </View>

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
  requiredStar: {
    fontSize: 14,
    fontWeight: '700',
  },
  fieldInlineError: {
    fontSize: 12,
    fontWeight: '600',
    lineHeight: 16,
    marginTop: 2,
    paddingHorizontal: 2,
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
  criteriaContainer: {
    gap: 6,
    marginTop: 6,
    paddingHorizontal: 2,
  },
  criterionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  criterionText: {
    fontSize: 12,
    fontWeight: '500',
    lineHeight: 16,
  },
  termsWrapper: {
    borderRadius: 12,
    padding: Spacing.two,
    gap: 6,
  },
  termsAgreementRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.two,
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
  termsWarningBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 4,
    paddingHorizontal: 4,
  },
  termsInlineError: {
    fontSize: 12,
    fontWeight: '600',
    lineHeight: 16,
    flex: 1,
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
