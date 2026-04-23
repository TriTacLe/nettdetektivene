import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRouter } from 'vue-router'
import AuthService from '@/services/AuthService.ts'
import type { UserDTO, FeideLoginRequest } from '@/types'
import { UserRole } from '@/types'

export const useAuthStore = defineStore(
  'auth',
  () => {
    const router = useRouter()

    const token = ref<string | null>(null)
    const refreshToken = ref<string | null>(null)
    const user = ref<UserDTO | null>(null)

    const isAuthenticated = computed(() => token.value !== null)
    const isTeacher = computed(() => user.value?.roles.includes(UserRole.TEACHER) ?? false)
    const isStudent = computed(() => user.value?.roles.includes(UserRole.STUDENT) ?? false)
    const userRole = computed<UserRole | null>(() => {
      if (isTeacher.value) return UserRole.TEACHER
      if (isStudent.value) return UserRole.STUDENT
      return null
    })

    async function feideLogin(credentials: FeideLoginRequest): Promise<void> {
      const response = await AuthService.feideLogin(credentials)
      sessionStorage.setItem('auth', JSON.stringify({ token: response.token, refreshToken: response.refreshToken }))
      token.value = response.token
      refreshToken.value = response.refreshToken
      await fetchMe()
    }

    async function fetchMe(): Promise<void> {
      user.value = await AuthService.fetchMe()
    }

    async function refresh(): Promise<void> {
      if (!refreshToken.value) {
        logout()
        return
      }
      const response = await AuthService.refresh(refreshToken.value, token.value ?? undefined)
      sessionStorage.setItem('auth', JSON.stringify({ token: response.token, refreshToken: response.refreshToken }))
      token.value = response.token
      refreshToken.value = response.refreshToken
    }

    function logout(): void {
      if (refreshToken.value) {
        AuthService.logout(refreshToken.value).catch(() => {})
      }
      sessionStorage.removeItem('auth')
      token.value = null
      refreshToken.value = null
      user.value = null
      router.push({ name: 'start' })
    }

    return {
      token,
      refreshToken,
      user,
      isAuthenticated,
      isTeacher,
      isStudent,
      userRole,
      feideLogin,
      fetchMe,
      refresh,
      logout,
    }
  },
  {
    persist: {
      storage: sessionStorage,
      pick: ['token', 'refreshToken', 'user'],
    },
  },
)
