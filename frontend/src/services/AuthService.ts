import apiClient from '@/services/ApiService'
import type { AuthResponse, FeideAuthResponse, FeideLoginRequest, LoginRequest, UserDTO } from "@/types";

async function login(credentials: LoginRequest): Promise<AuthResponse>{
  const { data } = await apiClient.post<AuthResponse>('/auth/login', credentials);
  return data;
}

async function feideLogin(credentials: FeideLoginRequest): Promise<FeideAuthResponse> {
  const { data } = await apiClient.post<FeideAuthResponse>(`/auth/feide`, credentials);
  return data
}

async function logout(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', { refreshToken })
}

async function refresh(refreshToken: string, expiredToken?: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/refresh', { refreshToken, expiredToken })
  return data
}

async function fetchMe(): Promise<UserDTO> {
  const { data } = await apiClient.get<UserDTO>('/users/me');
  return data;
}

export default {
  login,
  feideLogin,
  logout,
  refresh,
  fetchMe,
}
