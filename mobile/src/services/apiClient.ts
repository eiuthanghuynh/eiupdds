/**
 * Core API Client for EIU PDDS Mobile Client
 * Uses native fetch with AbortController timeout & Multipart FormData support.
 */

import Constants from 'expo-constants';
import { Platform } from 'react-native';

import { ApiResponse } from '@/types/diagnosis';

const DEFAULT_TIMEOUT_MS = 30000;

/**
 * Dynamically resolves the API base URL.
 * If EXPO_PUBLIC_API_URL contains 'localhost', this automatically resolves
 * to the computer's current Wi-Fi IP address via Expo Constants hostUri,
 * preventing connection breakage when switching Wi-Fi networks.
 */
export function resolveApiBaseUrl(): string {
  const envUrl = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

  // If running on web, localhost is directly accessible
  if (Platform.OS === 'web') {
    return envUrl;
  }

  // If already pointing to an explicit remote host, return as-is
  if (!envUrl.includes('localhost') && !envUrl.includes('127.0.0.1')) {
    return envUrl;
  }

  // On native device/emulator:
  // Dynamically resolve the host IP that Metro bundler is running on
  const hostUri =
    Constants.expoConfig?.hostUri ||
    (Constants as any).manifest2?.extra?.expoClient?.hostUri ||
    (Constants as any).manifest?.debuggerHost;

  if (hostUri) {
    const hostIp = hostUri.split(':')[0];
    if (hostIp && hostIp !== 'localhost' && hostIp !== '127.0.0.1') {
      return envUrl.replace(/localhost|127\.0\.0\.1/, hostIp);
    }
  }

  // Fallback for Android Emulator loopback if hostUri is unavailable
  if (Platform.OS === 'android') {
    return envUrl.replace(/localhost|127\.0\.0\.1/, '10.0.2.2');
  }

  return envUrl;
}

export class ApiError extends Error {
  code: string;
  status: number;
  details?: any;

  constructor(message: string, code: string = 'NETWORK_ERROR', status: number = 0, details?: any) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.details = details;
  }
}

async function requestWithTimeout<T>(
  url: string,
  options: RequestInit,
  timeoutMs: number = DEFAULT_TIMEOUT_MS
): Promise<T> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  const baseUrl = resolveApiBaseUrl();

  try {
    const fullUrl = url.startsWith('http') ? url : `${baseUrl}${url.startsWith('/') ? '' : '/'}${url}`;
    const response = await fetch(fullUrl, {
      ...options,
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      let errorMessage = data?.message;
      if (!errorMessage && data?.error) {
        errorMessage = data.error;
      }
      if (!errorMessage && data?.errors && Array.isArray(data.errors)) {
        errorMessage = data.errors.map((e: any) => e.defaultMessage || e).join(', ');
      }
      if (!errorMessage) {
        errorMessage = `Máy chủ phản hồi mã lỗi HTTP ${response.status}${response.statusText ? ` (${response.statusText})` : ''}`;
      }
      const errorCode = data?.code || `HTTP_${response.status}`;
      throw new ApiError(errorMessage, errorCode, response.status, data);
    }

    return data as T;
  } catch (error: any) {
    clearTimeout(timeoutId);

    if (error.name === 'AbortError') {
      throw new ApiError(
        `Quá thời gian chờ phản hồi từ máy chủ (> ${timeoutMs / 1000}s). Vui lòng kiểm tra lại kết nối mạng!`,
        'TIMEOUT',
        408
      );
    }
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(
      `Không thể kết nối đến máy chủ (${baseUrl}): ${error.message || 'Lỗi mạng'}. Vui lòng kiểm tra Docker backend và kết nối mạng.`,
      'CONNECTION_FAILED',
      0
    );
  }
}

export const ApiClient = {
  getBaseUrl(): string {
    return resolveApiBaseUrl();
  },

  async get<T>(endpoint: string, headers: Record<string, string> = {}): Promise<ApiResponse<T>> {
    return requestWithTimeout<ApiResponse<T>>(endpoint, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
        ...headers,
      },
    });
  },

  async post<T>(endpoint: string, body: any, headers: Record<string, string> = {}): Promise<ApiResponse<T>> {
    return requestWithTimeout<ApiResponse<T>>(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        ...headers,
      },
      body: JSON.stringify(body),
    });
  },

  async postMultipart<T>(
    endpoint: string,
    formData: FormData,
    headers: Record<string, string> = {}
  ): Promise<ApiResponse<T>> {
    // Note: Do NOT set Content-Type header manually for multipart/form-data so fetch will inject boundary automatically
    return requestWithTimeout<ApiResponse<T>>(endpoint, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        ...headers,
      },
      body: formData,
    });
  },
};
