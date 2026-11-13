import React, { useState } from 'react';
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';
import { router } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  Bell,
  Camera,
  Check,
  ChevronRight,
  FileText,
  Globe,
  Info,
  Moon,
  ShieldCheck,
  Sparkles,
  Sun,
  User,
  X,
} from 'lucide-react-native';

import { TitleComponent } from '@/components/common/TitleComponent';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { BottomTabInset, MaxContentWidth, Spacing } from '@/constants/theme';
import { useLanguage } from '@/context/LanguageContext';
import { useAppTheme } from '@/context/ThemeContext';

export default function ProfileScreen() {
  const { isDark, toggleTheme, colors } = useAppTheme();
  const { language, setLanguage, t } = useLanguage();
  const [notifications, setNotifications] = useState(true);
  const [autoSave, setAutoSave] = useState(true);
  const [languageModalVisible, setLanguageModalVisible] = useState(false);

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        {/* Fixed Header Title - Cố định ở góc trên, không bị cuộn đi */}
        <View style={styles.headerContainer}>
          <TitleComponent
            title={t('profileTitle')}
            subtitle={t('profileSubtitle')}
          />
        </View>

        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}>
          {/* User Info Card */}
          <ThemedView type="surface" style={[styles.card, { borderColor: colors.border }]}>
            <View style={styles.userRow}>
              <View
                style={[
                  styles.avatar,
                  {
                    backgroundColor: isDark ? '#2F3A10' : '#CFFF74',
                    borderColor: isDark ? '#CFFF74' : '#2F3A10',
                  },
                ]}>
                <User size={30} color={isDark ? '#CFFF74' : '#2F3A10'} />
              </View>
              <View style={styles.userInfo}>
                <ThemedText type="smallBold" style={styles.userName}>
                  {t('userName')}
                </ThemedText>
                <ThemedText type="small" themeColor="textSecondary">
                  pdds.user@eiu.edu.vn
                </ThemedText>
                <View
                  style={[
                    styles.roleBadge,
                    {
                      backgroundColor: isDark
                        ? 'rgba(207, 255, 116, 0.15)'
                        : 'rgba(47, 58, 16, 0.1)',
                    },
                  ]}>
                  <Sparkles size={12} color={isDark ? '#CFFF74' : '#2F3A10'} />
                  <ThemedText
                    type="small"
                    style={[
                      styles.roleText,
                      { color: isDark ? '#CFFF74' : '#2F3A10' },
                    ]}>
                    {t('userRole')}
                  </ThemedText>
                </View>
              </View>
            </View>

            {/* Quick Auth Navigation Button */}
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Đăng nhập hoặc Đăng ký tài khoản"
              style={({ pressed }) => [
                styles.authActionBtn,
                { backgroundColor: '#CFFF74' },
                pressed && styles.pressed,
              ]}
              onPress={() => router.push('/login' as any)}>
              <ThemedText style={styles.authActionBtnText}>
                Đăng nhập / Đổi tài khoản
              </ThemedText>
              <ChevronRight size={16} color="#2F3A10" />
            </Pressable>
          </ThemedView>

          {/* Section: App Settings */}
          <View style={styles.sectionContainer}>
            <ThemedText type="smallBold" style={styles.sectionTitle}>
              {t('appSettings')}
            </ThemedText>

            <ThemedView type="surface" style={[styles.card, { borderColor: colors.border }]}>
              {/* Setting 1: Dark Mode Toggle */}
              <Pressable
                accessibilityRole="switch"
                accessibilityLabel="Chuyển đổi giao diện sáng tối"
                onPress={toggleTheme}
                style={({ pressed }) => [
                  styles.settingRow,
                  pressed && styles.pressed,
                ]}>
                <View style={styles.settingRowLeft}>
                  <View
                    style={[
                      styles.iconCircle,
                      {
                        backgroundColor: isDark ? '#2F3A10' : '#DFE5D0',
                      },
                    ]}>
                    {isDark ? (
                      <Moon size={18} color="#CFFF74" />
                    ) : (
                      <Sun size={18} color="#2F3A10" />
                    )}
                  </View>
                  <View style={styles.settingTexts}>
                    <ThemedText type="smallBold" style={styles.settingTitle}>
                      {t('darkMode')}
                    </ThemedText>
                    <ThemedText type="small" themeColor="textSecondary">
                      {isDark ? t('darkModeOn') : t('darkModeOff')}
                    </ThemedText>
                  </View>
                </View>

                {/* Switch Component */}
                <View
                  style={[
                    styles.switchTrack,
                    {
                      backgroundColor: isDark ? '#2F3A10' : '#D2D9C2',
                      borderColor: colors.border,
                    },
                  ]}>
                  <View
                    style={[
                      styles.switchThumb,
                      isDark ? styles.switchThumbDark : styles.switchThumbLight,
                      { backgroundColor: isDark ? '#CFFF74' : '#ffffff' },
                    ]}>
                    {isDark ? (
                      <Moon size={12} color="#0E1308" />
                    ) : (
                      <Sun size={12} color="#2F3A10" />
                    )}
                  </View>
                </View>
              </Pressable>

              <View style={[styles.divider, { backgroundColor: colors.border }]} />

              {/* Setting 2: Language Switcher */}
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Chọn ngôn ngữ"
                onPress={() => setLanguageModalVisible(true)}
                style={({ pressed }) => [
                  styles.settingRow,
                  pressed && styles.pressed,
                ]}>
                <View style={styles.settingRowLeft}>
                  <View
                    style={[
                      styles.iconCircle,
                      {
                        backgroundColor: isDark ? '#2F3A10' : '#DFE5D0',
                      },
                    ]}>
                    <Globe size={18} color={isDark ? '#CFFF74' : '#2F3A10'} />
                  </View>
                  <View style={styles.settingTexts}>
                    <ThemedText type="smallBold" style={styles.settingTitle}>
                      {t('language')}
                    </ThemedText>
                    <ThemedText type="small" themeColor="textSecondary">
                      {language === 'vi' ? 'Tiếng Việt' : 'English'}
                    </ThemedText>
                  </View>
                </View>

                {/* Language Quick Toggle Pills */}
                <View
                  style={[
                    styles.langToggleContainer,
                    {
                      backgroundColor: isDark ? '#243015' : '#D2D9C2',
                      borderColor: colors.border,
                    },
                  ]}>
                  <Pressable
                    onPress={() => setLanguage('vi')}
                    style={[
                      styles.langPill,
                      language === 'vi' && [
                        styles.langPillActive,
                        { backgroundColor: isDark ? '#CFFF74' : '#2F3A10' },
                      ],
                    ]}>
                    <ThemedText
                      type="small"
                      style={[
                        styles.langPillText,
                        language === 'vi'
                          ? { color: isDark ? '#0E1308' : '#ffffff', fontWeight: '700' }
                          : { color: colors.textSecondary },
                      ]}>
                      VI
                    </ThemedText>
                  </Pressable>

                  <Pressable
                    onPress={() => setLanguage('en')}
                    style={[
                      styles.langPill,
                      language === 'en' && [
                        styles.langPillActive,
                        { backgroundColor: isDark ? '#CFFF74' : '#2F3A10' },
                      ],
                    ]}>
                    <ThemedText
                      type="small"
                      style={[
                        styles.langPillText,
                        language === 'en'
                          ? { color: isDark ? '#0E1308' : '#ffffff', fontWeight: '700' }
                          : { color: colors.textSecondary },
                      ]}>
                      EN
                    </ThemedText>
                  </Pressable>
                </View>
              </Pressable>

              <View style={[styles.divider, { backgroundColor: colors.border }]} />

              {/* Setting 3: Notifications */}
              <Pressable
                accessibilityRole="switch"
                accessibilityLabel="Bật tắt thông báo"
                onPress={() => setNotifications((v) => !v)}
                style={({ pressed }) => [
                  styles.settingRow,
                  pressed && styles.pressed,
                ]}>
                <View style={styles.settingRowLeft}>
                  <View
                    style={[
                      styles.iconCircle,
                      {
                        backgroundColor: isDark ? '#2F3A10' : '#DFE5D0',
                      },
                    ]}>
                    <Bell size={18} color={isDark ? '#CFFF74' : '#2F3A10'} />
                  </View>
                  <View style={styles.settingTexts}>
                    <ThemedText type="smallBold" style={styles.settingTitle}>
                      {t('notifications')}
                    </ThemedText>
                    <ThemedText type="small" themeColor="textSecondary">
                      {t('notificationsDesc')}
                    </ThemedText>
                  </View>
                </View>

                <View
                  style={[
                    styles.switchTrack,
                    {
                      backgroundColor: notifications
                        ? isDark
                          ? '#2F3A10'
                          : '#2F3A10'
                        : '#D2D9C2',
                      borderColor: colors.border,
                    },
                  ]}>
                  <View
                    style={[
                      styles.switchThumb,
                      notifications ? styles.switchThumbDark : styles.switchThumbLight,
                      { backgroundColor: notifications ? '#CFFF74' : '#ffffff' },
                    ]}
                  />
                </View>
              </Pressable>

              <View style={[styles.divider, { backgroundColor: colors.border }]} />

              {/* Setting 4: Auto-Save Photos */}
              <Pressable
                accessibilityRole="switch"
                accessibilityLabel="Tự động lưu ảnh chẩn đoán"
                onPress={() => setAutoSave((v) => !v)}
                style={({ pressed }) => [
                  styles.settingRow,
                  pressed && styles.pressed,
                ]}>
                <View style={styles.settingRowLeft}>
                  <View
                    style={[
                      styles.iconCircle,
                      {
                        backgroundColor: isDark ? '#2F3A10' : '#DFE5D0',
                      },
                    ]}>
                    <Camera size={18} color={isDark ? '#CFFF74' : '#2F3A10'} />
                  </View>
                  <View style={styles.settingTexts}>
                    <ThemedText type="smallBold" style={styles.settingTitle}>
                      {t('autoSave')}
                    </ThemedText>
                    <ThemedText type="small" themeColor="textSecondary">
                      {t('autoSaveDesc')}
                    </ThemedText>
                  </View>
                </View>

                <View
                  style={[
                    styles.switchTrack,
                    {
                      backgroundColor: autoSave
                        ? isDark
                          ? '#2F3A10'
                          : '#2F3A10'
                        : '#D2D9C2',
                      borderColor: colors.border,
                    },
                  ]}>
                  <View
                    style={[
                      styles.switchThumb,
                      autoSave ? styles.switchThumbDark : styles.switchThumbLight,
                      { backgroundColor: autoSave ? '#CFFF74' : '#ffffff' },
                    ]}
                  />
                </View>
              </Pressable>
            </ThemedView>
          </View>

          {/* Section: Thông tin ứng dụng */}
          <View style={styles.sectionContainer}>
            <ThemedText type="smallBold" style={styles.sectionTitle}>
              {t('appInformation')}
            </ThemedText>

            <ThemedView type="surface" style={[styles.card, { borderColor: colors.border }]}>
              {/* 1. Điều khoản sử dụng ứng dụng và dịch vụ */}
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={t('termsOfService')}
                style={({ pressed }) => [
                  styles.settingRow,
                  pressed && styles.pressed,
                ]}>
                <View style={styles.settingRowLeft}>
                  <View
                    style={[
                      styles.iconCircle,
                      { backgroundColor: isDark ? '#2F3A10' : '#DFE5D0' },
                    ]}>
                    <FileText size={18} color={isDark ? '#CFFF74' : '#2F3A10'} />
                  </View>
                  <View style={styles.settingTexts}>
                    <ThemedText type="smallBold" style={styles.settingTitle}>
                      {t('termsOfService')}
                    </ThemedText>
                  </View>
                </View>
                <ChevronRight size={18} color={colors.textSecondary} />
              </Pressable>

              <View style={[styles.divider, { backgroundColor: colors.border }]} />

              {/* 2. Chính sách quyền riêng tư */}
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={t('privacyPolicy')}
                style={({ pressed }) => [
                  styles.settingRow,
                  pressed && styles.pressed,
                ]}>
                <View style={styles.settingRowLeft}>
                  <View
                    style={[
                      styles.iconCircle,
                      { backgroundColor: isDark ? '#2F3A10' : '#DFE5D0' },
                    ]}>
                    <ShieldCheck size={18} color={isDark ? '#CFFF74' : '#2F3A10'} />
                  </View>
                  <View style={styles.settingTexts}>
                    <ThemedText type="smallBold" style={styles.settingTitle}>
                      {t('privacyPolicy')}
                    </ThemedText>
                  </View>
                </View>
                <ChevronRight size={18} color={colors.textSecondary} />
              </Pressable>

              <View style={[styles.divider, { backgroundColor: colors.border }]} />

              {/* 3. Phiên bản ứng dụng */}
              <View style={styles.settingRow}>
                <View style={styles.settingRowLeft}>
                  <View
                    style={[
                      styles.iconCircle,
                      { backgroundColor: isDark ? '#2F3A10' : '#DFE5D0' },
                    ]}>
                    <Info size={18} color={isDark ? '#CFFF74' : '#2F3A10'} />
                  </View>
                  <View style={styles.settingTexts}>
                    <ThemedText type="smallBold" style={styles.settingTitle}>
                      {t('appVersion')}
                    </ThemedText>
                  </View>
                </View>
                <ThemedText type="smallBold">1.0.0</ThemedText>
              </View>
            </ThemedView>
          </View>
        </ScrollView>
      </SafeAreaView>

      {/* Language Selection Modal */}
      <Modal
        visible={languageModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setLanguageModalVisible(false)}>
        <Pressable
          style={styles.modalBackdrop}
          onPress={() => setLanguageModalVisible(false)}>
          <ThemedView
            type="surface"
            style={[
              styles.modalCard,
              { borderColor: colors.border },
            ]}>
            <View style={styles.modalHeader}>
              <ThemedText type="smallBold" style={styles.modalTitle}>
                {t('selectLanguage')}
              </ThemedText>
              <Pressable
                onPress={() => setLanguageModalVisible(false)}
                hitSlop={8}
                style={styles.modalCloseBtn}>
                <X size={20} color={colors.text} />
              </Pressable>
            </View>

            {/* Vietnamese Option */}
            <Pressable
              onPress={() => {
                setLanguage('vi');
                setLanguageModalVisible(false);
              }}
              style={({ pressed }) => [
                styles.modalOption,
                language === 'vi' && {
                  backgroundColor: isDark ? '#243015' : '#D2D9C2',
                },
                pressed && styles.pressed,
              ]}>
              <View style={styles.modalOptionLeft}>
                <ThemedText style={styles.flagEmoji}>🇻🇳</ThemedText>
                <View>
                  <ThemedText type="smallBold">Tiếng Việt</ThemedText>
                  <ThemedText type="small" themeColor="textSecondary">
                    Vietnamese
                  </ThemedText>
                </View>
              </View>
              {language === 'vi' && <Check size={20} color={isDark ? '#CFFF74' : '#2F3A10'} />}
            </Pressable>

            {/* English Option */}
            <Pressable
              onPress={() => {
                setLanguage('en');
                setLanguageModalVisible(false);
              }}
              style={({ pressed }) => [
                styles.modalOption,
                language === 'en' && {
                  backgroundColor: isDark ? '#243015' : '#D2D9C2',
                },
                pressed && styles.pressed,
              ]}>
              <View style={styles.modalOptionLeft}>
                <ThemedText style={styles.flagEmoji}>🇬🇧</ThemedText>
                <View>
                  <ThemedText type="smallBold">English</ThemedText>
                  <ThemedText type="small" themeColor="textSecondary">
                    Tiếng Anh
                  </ThemedText>
                </View>
              </View>
              {language === 'en' && <Check size={20} color={isDark ? '#CFFF74' : '#2F3A10'} />}
            </Pressable>
          </ThemedView>
        </Pressable>
      </Modal>
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
    paddingHorizontal: Spacing.four,
    paddingTop: Spacing.two,
    maxWidth: MaxContentWidth,
    width: '100%',
  },
  headerContainer: {
    width: '100%',
    paddingBottom: Spacing.two,
    zIndex: 10,
  },
  scrollView: {
    flex: 1,
    width: '100%',
  },
  scrollContent: {
    paddingTop: Spacing.one,
    paddingBottom: BottomTabInset + Spacing.five,
    gap: Spacing.four,
  },
  card: {
    borderRadius: Spacing.four,
    padding: Spacing.three,
    gap: Spacing.two,
    borderWidth: 1,
  },
  userRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
  },
  avatar: {
    width: 60,
    height: 60,
    borderRadius: 30,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
  },
  userInfo: {
    flex: 1,
    gap: 2,
  },
  userName: {
    fontSize: 18,
    lineHeight: 22,
  },
  roleBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    alignSelf: 'flex-start',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 12,
    marginTop: 4,
  },
  roleText: {
    fontSize: 11,
    fontWeight: '700',
  },
  sectionContainer: {
    gap: Spacing.two,
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: '700',
    paddingHorizontal: Spacing.one,
  },
  settingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.one,
  },
  settingRowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    flex: 1,
  },
  iconCircle: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  settingTexts: {
    flex: 1,
    gap: 1,
  },
  settingTitle: {
    fontSize: 15,
  },
  switchTrack: {
    width: 50,
    height: 28,
    borderRadius: 14,
    padding: 3,
    borderWidth: 1,
    justifyContent: 'center',
  },
  switchThumb: {
    width: 20,
    height: 20,
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.15,
    shadowRadius: 2,
    elevation: 2,
  },
  switchThumbLight: {
    transform: [{ translateX: 0 }],
  },
  switchThumbDark: {
    transform: [{ translateX: 22 }],
  },
  langToggleContainer: {
    flexDirection: 'row',
    borderRadius: 14,
    padding: 2,
    borderWidth: 1,
  },
  langPill: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  langPillActive: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 1,
  },
  langPillText: {
    fontSize: 11,
  },
  divider: {
    height: 1,
    opacity: 0.6,
    marginVertical: Spacing.half,
  },
  pressed: {
    opacity: 0.75,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.four,
  },
  modalCard: {
    width: '100%',
    maxWidth: 360,
    borderRadius: Spacing.four,
    padding: Spacing.four,
    gap: Spacing.three,
    borderWidth: 1,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  modalTitle: {
    fontSize: 17,
  },
  modalCloseBtn: {
    padding: 4,
  },
  modalOption: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: Spacing.three,
    borderRadius: Spacing.three,
  },
  modalOptionLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
  },
  flagEmoji: {
    fontSize: 24,
  },
  authActionBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    height: 44,
    borderRadius: 12,
    marginTop: Spacing.three,
    gap: Spacing.one,
    paddingHorizontal: Spacing.three,
  },
  authActionBtnText: {
    color: '#2F3A10',
    fontSize: 14,
    fontWeight: '700',
  },
});
