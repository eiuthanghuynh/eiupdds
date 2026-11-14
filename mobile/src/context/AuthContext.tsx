import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Platform } from 'react-native';

import { AuthService } from '@/services/authService';
import { AuthResponse, LoginPayload, RegisterPayload, UserSummary } from '@/types/auth';

const STORAGE_TOKEN_KEY = 'eiu_pdds_access_token';
const STORAGE_USER_KEY = 'eiu_pdds_user_session';

export interface AuthContextType {
  user: UserSummary | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  login: (payload: LoginPayload) => Promise<AuthResponse>;
  register: (payload: RegisterPayload) => Promise<AuthResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

function getInitialSession(): { user: UserSummary | null; token: string | null } {
  if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
    try {
      const savedToken = window.localStorage.getItem(STORAGE_TOKEN_KEY);
      const savedUser = window.localStorage.getItem(STORAGE_USER_KEY);
      return {
        token: savedToken || null,
        user: savedUser ? JSON.parse(savedUser) : null,
      };
    } catch {
      // ignore storage access error
    }
  }
  return { user: null, token: null };
}

function persistSession(token: string | null, user: UserSummary | null) {
  if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
    try {
      if (token) {
        window.localStorage.setItem(STORAGE_TOKEN_KEY, token);
      } else {
        window.localStorage.removeItem(STORAGE_TOKEN_KEY);
      }
      if (user) {
        window.localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(user));
      } else {
        window.localStorage.removeItem(STORAGE_USER_KEY);
      }
    } catch {
      // ignore storage access error
    }
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const initial = getInitialSession();
  const [user, setUser] = useState<UserSummary | null>(initial.user);
  const [accessToken, setAccessToken] = useState<string | null>(initial.token);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);

  const login = useCallback(async (payload: LoginPayload): Promise<AuthResponse> => {
    const authData = await AuthService.login(payload);
    setUser(authData.user);
    setAccessToken(authData.accessToken);
    setRefreshToken(authData.refreshToken);
    persistSession(authData.accessToken, authData.user);
    return authData;
  }, []);

  const register = useCallback(async (payload: RegisterPayload): Promise<AuthResponse> => {
    const authData = await AuthService.register(payload);
    setUser(authData.user);
    setAccessToken(authData.accessToken);
    setRefreshToken(authData.refreshToken);
    persistSession(authData.accessToken, authData.user);
    return authData;
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    setAccessToken(null);
    setRefreshToken(null);
    persistSession(null, null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      accessToken,
      refreshToken,
      isAuthenticated: !!user && !!accessToken,
      login,
      register,
      logout,
    }),
    [user, accessToken, refreshToken, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    return {
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      login: async () => {
        throw new Error('AuthProvider is missing');
      },
      register: async () => {
        throw new Error('AuthProvider is missing');
      },
      logout: () => {},
    };
  }
  return context;
}
