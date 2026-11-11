import type { ReactNode } from 'react';
import { View, StyleSheet } from 'react-native';

import { ThemedText } from './themed-text';
import { ThemedView } from './themed-view';

import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

type HintRowProps = {
  title?: string;
  hint?: ReactNode;
};

export function HintRow({ title = 'Try editing', hint = 'app/index.tsx' }: HintRowProps) {
  const theme = useTheme();

  return (
    <View style={styles.stepRow}>
      <ThemedText type="small">{title}</ThemedText>
      <ThemedView type="background" style={[styles.codeSnippet, { borderColor: theme.border }]}>
        <ThemedText type="code">{hint}</ThemedText>
      </ThemedView>
    </View>
  );
}

const styles = StyleSheet.create({
  stepRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  codeSnippet: {
    borderRadius: Spacing.two,
    paddingVertical: Spacing.half,
    paddingHorizontal: Spacing.two,
    borderWidth: 1,
  },
});
