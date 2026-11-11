import {
  Tabs,
  TabList,
  TabTrigger,
  TabSlot,
  TabTriggerSlotProps,
  TabListProps,
} from 'expo-router/ui';
import { Home, History, Camera, Bell, User } from 'lucide-react-native';
import { Pressable, View, StyleSheet } from 'react-native';

import { ThemedText } from './themed-text';
import { MaxContentWidth, Spacing } from '@/constants/theme';
import { useLanguage } from '@/context/LanguageContext';
import { useAppTheme } from '@/context/ThemeContext';

export default function AppTabs() {
  const { t } = useLanguage();

  return (
    <Tabs style={styles.tabsContainer}>
      <TabSlot style={styles.tabSlot} />
      <TabList asChild>
        <CustomTabList>
          <TabTrigger name="index" href={'/' as any} asChild>
            <TabButton icon={Home} label={t('tabHome')} />
          </TabTrigger>
          <TabTrigger name="history" href="/history" asChild>
            <TabButton icon={History} label={t('tabHistory')} />
          </TabTrigger>
          <TabTrigger name="camera" href="/camera" asChild>
            <TabButton icon={Camera} isSpecial />
          </TabTrigger>
          <TabTrigger name="notification" href="/notification" asChild>
            <TabButton icon={Bell} label={t('tabNotification')} />
          </TabTrigger>
          <TabTrigger name="profile" href="/profile" asChild>
            <TabButton icon={User} label={t('tabProfile')} />
          </TabTrigger>
        </CustomTabList>
      </TabList>
    </Tabs>
  );
}

interface TabButtonProps extends TabTriggerSlotProps {
  icon: React.ComponentType<{ size: number; color: string }>;
  label?: string;
  isSpecial?: boolean;
}

export function TabButton({
  icon: IconComponent,
  label,
  isSpecial,
  isFocused,
  ...props
}: TabButtonProps) {
  const { colors, isDark } = useAppTheme();
  const iconColor = isFocused ? '#ffffff' : colors.text;

  if (isSpecial) {
    return (
      <Pressable
        {...props}
        style={({ pressed }) => [
          styles.specialButtonContainer,
          pressed && styles.pressed,
        ]}>
        <View
          style={[
            styles.specialButtonInner,
            {
              backgroundColor: colors.backgroundSelected,
              borderColor: isDark ? colors.primary : colors.navBackground,
            },
          ]}>
          <IconComponent size={26} color="#ffffff" />
        </View>
      </Pressable>
    );
  }

  return (
    <Pressable
      {...props}
      style={({ pressed }) => [
        styles.tabButton,
        isFocused && { backgroundColor: colors.backgroundSelected },
        pressed && styles.pressed,
      ]}>
      <IconComponent size={20} color={iconColor} />
      {label && (
        <ThemedText
          type="small"
          numberOfLines={1}
          style={[
            styles.tabLabel,
            isFocused ? styles.tabLabelFocused : { color: colors.textSecondary },
          ]}>
          {label}
        </ThemedText>
      )}
    </Pressable>
  );
}

export function CustomTabList(props: TabListProps) {
  const { colors } = useAppTheme();

  return (
    <View
      {...props}
      style={[
        styles.tabListContainer,
        {
          backgroundColor: colors.navBackground,
          borderTopColor: colors.border,
        },
      ]}>
      <View style={styles.innerContainer}>{props.children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  tabsContainer: {
    flex: 1,
    height: '100%',
  },
  tabSlot: {
    flex: 1,
    height: '100%',
  },
  tabListContainer: {
    position: 'fixed' as any,
    bottom: 0,
    left: 0,
    right: 0,
    width: '100%',
    borderTopWidth: 1,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 9999,
  },
  innerContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
    maxWidth: MaxContentWidth,
    paddingVertical: Spacing.one,
    paddingHorizontal: Spacing.one,
  },
  tabButton: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 6,
    paddingHorizontal: 2,
    borderRadius: Spacing.three,
    gap: 3,
  },
  specialButtonContainer: {
    width: 60,
    alignItems: 'center',
    justifyContent: 'center',
    transform: [{ translateY: -16 }],
  },
  specialButtonInner: {
    width: 54,
    height: 54,
    borderRadius: 27,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 6,
    elevation: 6,
  },
  pressed: {
    opacity: 0.7,
  },
  tabLabel: {
    fontSize: 10.5,
    lineHeight: 13,
    textAlign: 'center',
  },
  tabLabelFocused: {
    fontWeight: '700',
    color: '#ffffff',
  },
  tabLabelUnfocused: {
    fontWeight: '600',
    color: '#2F3A10',
  },
});
