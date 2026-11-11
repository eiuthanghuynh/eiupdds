import { useAppTheme } from '@/context/ThemeContext';

export function useColorScheme() {
  const { mode } = useAppTheme();
  return mode;
}
