import { ApiClient } from './apiClient';
import { AuthResponse, LoginPayload, RegisterPayload } from '@/types/auth';

export const AuthService = {
  /**
   * Authenticate user with email or username and password against live backend
   */
  async login(payload: LoginPayload): Promise<AuthResponse> {
    const response = await ApiClient.post<AuthResponse>('/auth/login', payload);
    if (!response.data) {
      throw new Error(response.message || 'Đăng nhập không thành công. Dữ liệu phản hồi trống.');
    }
    return response.data;
  },

  /**
   * Register a new user account against live backend
   */
  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const response = await ApiClient.post<AuthResponse>('/auth/register', payload);
    if (!response.data) {
      throw new Error(response.message || 'Đăng ký không thành công. Dữ liệu phản hồi trống.');
    }
    return response.data;
  },
};
