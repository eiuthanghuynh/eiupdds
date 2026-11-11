import React from 'react';
import {
    StyleSheet,
    View,
    type StyleProp,
    type TextStyle,
    type ViewStyle,
} from 'react-native';

import { ThemedText, type ThemedTextProps } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';

export interface TitleComponentProps {
    title?: string;
    subtitle?: string;
    type?: ThemedTextProps['type'];
    align?: 'left' | 'center' | 'right';
    style?: StyleProp<ViewStyle>;
    titleStyle?: StyleProp<TextStyle>;
    subtitleStyle?: StyleProp<TextStyle>;
    children?: React.ReactNode;
}

export function TitleComponent({
    title,
    subtitle,
    type = 'smallBold',
    align = 'left',
    style,
    titleStyle,
    subtitleStyle,
    children,
}: TitleComponentProps) {
    const content = title ?? children;

    return (
        <View style={[styles.container, alignStyles[align], style]}>
            {typeof content === 'string' ? (
                <ThemedText
                    type={type}
                    style={[styles.title, textAlignStyles[align], titleStyle]}>
                    {content}
                </ThemedText>
            ) : (
                content
            )}
            {subtitle ? (
                <ThemedText
                    type="small"
                    themeColor="textSecondary"
                    style={[styles.subtitle, textAlignStyles[align], subtitleStyle]}>
                    {subtitle}
                </ThemedText>
            ) : null}
        </View>
    );
}

const alignStyles = {
    left: { alignItems: 'flex-start' as const },
    center: { alignItems: 'center' as const },
    right: { alignItems: 'flex-end' as const },
};

const textAlignStyles = {
    left: { textAlign: 'left' as const },
    center: { textAlign: 'center' as const },
    right: { textAlign: 'right' as const },
};

const styles = StyleSheet.create({
    container: {
        width: '100%',
        paddingVertical: Spacing.two,
        gap: Spacing.half,
    },
    title: {
        fontSize: 20,
        fontWeight: '700',
        lineHeight: 20,
    },
    subtitle: {
        fontSize: 12,
        lineHeight: 16,
    },
});

export default TitleComponent;
