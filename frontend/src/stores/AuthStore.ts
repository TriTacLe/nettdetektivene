import { ref, computed } from 'vue'
import { defineStore } from "pinia";
import { useRouter } from 'vue-router'
import apiClient from "@/services/ApiService.ts";
import type { UserDTO, LoginRequest, AuthResponse } from "@/types";
import { UserRole } from "@/types";

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter();

  const token = ref<string | null>(null);
  const refreshToken = ref<string | null>(null);
  const user = ref<UserDTO | null>(null);

  const isAuthenticated = computed(() => token.value !== null);
  const isTeacher = computed(() => user.value?.roles.includes(UserRole.TEACHER) ?? false);
  const isStudent = computed(() => user.value?.roles.includes(UserRole.STUDENT) ?? false);
  const userRole = computed<UserRole | null>(() => {
    if (isTeacher.value) return UserRole.TEACHER;
    if (isStudent.value) return UserRole.STUDENT;
    return null;
  })

  async function login(credentials: LoginRequest): Promise<void> {
    const { data } = await apiClient.post<AuthResponse>('/auth/login', credentials);
    token.value = data.token;
    refreshToken.value = data.refreshToken
    await fetchMe()
  }

  async function fetchMe(): Promise<void> {
    const { data } = await apiClient.get<UserDTO>('/users/me');
    user.value = data;
  }

  async function refresh(): Promise<void> {
    if (!refreshToken.value) {
      logout()
      return;
    }
    const { data } = await apiClient.post<AuthResponse>('/auth/refresh', {
      refreshToken: refreshToken.value,
    });
    token.value = data.token;
    refreshToken.value = data.refreshToken;
  }

  function logout(): void {
    if (refreshToken.value) {
      apiClient.post(`/auth/logout`, { refreshToken: refreshToken.value }).catch(() => {})
    }
    token.value = null;
    refreshToken.value = null;
    user.value = null;
    router.push( { name: 'login' } );
  }

  return {
    token,
    refreshToken,
    user,
    isAuthenticated,
    isTeacher,
    isStudent,
    userRole,
    login,
    fetchMe,
    refresh,
    logout,
  }
},
  {
    persist: {
      storage: sessionStorage,
      pick: ['token', 'refreshToken', 'user'],
    }
  }
)
