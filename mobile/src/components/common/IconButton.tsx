import React from 'react';
import {
  Pressable,
  StyleSheet,
  Text,
  type GestureResponderEvent,
  type PressableProps,
  type StyleProp,
  type TextStyle,
  type ViewStyle,
} from 'react-native';

import { Palette, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export type IconButtonIcon =
  | React.ComponentType<{ size?: number; color?: string; pressed?: boolean }>
  | ((props: { size?: number; color?: string; pressed?: boolean }) => React.ReactNode)
  | React.ReactNode;

export interface IconButtonProps extends Omit<PressableProps, 'style' | 'children'> {
  icon: IconButtonIcon;
  title: string;
  iconSize?: number;
  isSelected?: boolean;
  defaultColor?: string;
  activeColor?: string;
  defaultBackgroundColor?: string;
  activeBackgroundColor?: string;
  style?: StyleProp<ViewStyle> | ((state: { pressed: boolean }) => StyleProp<ViewStyle>);
  titleStyle?: StyleProp<TextStyle>;
  onPress?: (event: GestureResponderEvent) => void;
}

function renderIcon(
  icon: IconButtonIcon,
  color: string,
  size: number,
  pressed: boolean
): React.ReactNode {
  if (!icon) return null;

  if (React.isValidElement(icon)) {
    return React.cloneElement(icon as React.ReactElement<{ color?: string; size?: number }>, {
      color: (icon.props as { color?: string }).color ?? color,
      size: (icon.props as { size?: number }).size ?? size,
    });
  }

  if (typeof icon === 'function') {
    const IconComponent = icon as React.ComponentType<{
      size?: number;
      color?: string;
      pressed?: boolean;
    }>;
    return <IconComponent size={size} color={color} pressed={pressed} />;
  }

  return null;
}

export function IconButton({
  icon,
  title,
  iconSize = 24,
  isSelected = false,
  disabled = false,
  defaultColor,
  activeColor = '#ffffff',
  defaultBackgroundColor = 'transparent',
  activeBackgroundColor = Palette.secondary,
  style,
  titleStyle,
  onPress,
  ...pressableProps
}: IconButtonProps) {
  const theme = useTheme();
  const effectiveDefaultColor = defaultColor ?? theme.text;
  return (
    <Pressable
      disabled={disabled}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={title}
      style={({ pressed }) => {
        const isHighlight = pressed || isSelected;
        return [
          styles.button,
          {
            backgroundColor: isHighlight ? activeBackgroundColor : defaultBackgroundColor,
          },
          typeof style === 'function' ? style({ pressed }) : style,
          disabled && styles.disabled,
        ];
      }}
      {...pressableProps}>
      {({ pressed }) => {
        const isHighlight = pressed || isSelected;
        const currentColor = isHighlight ? activeColor : effectiveDefaultColor;

        return (
          <>
            {renderIcon(icon, currentColor, iconSize, pressed)}
            <Text
              style={[styles.title, { color: currentColor }, titleStyle]}
              numberOfLines={1}>
              {title}
            </Text>
          </>
        );
      }}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Spacing.two,
    paddingHorizontal: Spacing.three,
    borderRadius: Spacing.three,
    gap: Spacing.one,
  },
  title: {
    fontSize: 13,
    fontWeight: '600',
    textAlign: 'center',
  },
  disabled: {
    opacity: 0.5,
  },
});

export default IconButton;
