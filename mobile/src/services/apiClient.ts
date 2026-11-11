/**
 * Core API Client for EIU PDDS Mobile Client
 * Uses native fetch with AbortController timeout & Multipart FormData support.
 */

import { ApiResponse } from '@/types/diagnosis';

const DEFAULT_TIMEOUT_MS = 30000;
const BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

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

  try {
    const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`;
    const response = await fetch(fullUrl, {
      ...options,
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      const errorMessage = data?.message || `Máy chủ phản hồi mã lỗi HTTP ${response.status}`;
      const errorCode = data?.code || `HTTP_${response.status}`;
      throw new ApiError(errorMessage, errorCode, response.status, data);
    }

    return data as T;
  } catch (error: any) {
    clearTimeout(timeoutId);

    if (error.name === 'AbortError') {
      throw new ApiError('Quá thời gian kết nối đến máy chủ (> 15 giây). Vui lòng kiểm tra lại mạng!', 'TIMEOUT', 408);
    }
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(
      error.message || 'Không thể kết nối đến máy chủ Backend PDDS. Vui lòng kiểm tra mạng hoặc máy chủ.',
      'CONNECTION_FAILED',
      0
    );
  }
}

export const ApiClient = {
  getBaseUrl(): string {
    return BASE_URL;
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
