export interface UserSummary {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  role?: string;
  avatarUrl?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface LoginPayload {
  emailOrUsername: string;
  password: string;
  deviceInfo?: string;
}

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  fullName?: string;
  phoneNumber?: string;
}
