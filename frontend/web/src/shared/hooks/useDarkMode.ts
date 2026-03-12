import { useEffect, useState } from 'react';

type Theme = 'light' | 'dark' | 'system';

interface UseDarkModeOptions {
  /**
   * 세션 토큰 (제공시 세션별 독립 테마 저장)
   */
  sessionToken?: string;
}

/**
 * 다크모드 훅
 * - localStorage에 테마 설정 저장
 * - 시스템 설정 감지
 * - <html> 태그에 'dark' 클래스 토글
 * - sessionToken 제공시 세션별 독립 테마 관리
 */
export function useDarkMode(options?: UseDarkModeOptions) {
  const storageKey = options?.sessionToken ? `theme_${options.sessionToken}` : 'theme';

  const [theme, setTheme] = useState<Theme>(() => {
    // localStorage에서 테마 설정 복원
    const saved = localStorage.getItem(storageKey) as Theme | null;
    return saved || 'system';
  });

  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    const root = document.documentElement;

    // 시스템 다크모드 설정 확인
    const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

    // 실제 적용할 다크모드 여부 결정
    const shouldBeDark = theme === 'dark' || (theme === 'system' && systemPrefersDark);

    setIsDark(shouldBeDark);

    // <html> 태그에 'dark' 클래스 토글
    if (shouldBeDark) {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }

    // localStorage에 저장
    localStorage.setItem(storageKey, theme);
  }, [theme, storageKey]);

  // 시스템 설정 변경 감지
  useEffect(() => {
    if (theme !== 'system') return;

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const handleChange = (e: MediaQueryListEvent) => {
      setIsDark(e.matches);
      if (e.matches) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    };

    mediaQuery.addEventListener('change', handleChange);

    return () => {
      mediaQuery.removeEventListener('change', handleChange);
    };
  }, [theme]);

  const toggleDarkMode = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  const setLightMode = () => setTheme('light');
  const setDarkMode = () => setTheme('dark');
  const setSystemMode = () => setTheme('system');

  return {
    theme,
    isDark,
    toggleDarkMode,
    setLightMode,
    setDarkMode,
    setSystemMode,
  };
}
