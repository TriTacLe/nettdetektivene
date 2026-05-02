import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRouter } from 'vue-router'
import AuthService from '@/services/AuthService.ts'
import type { UserDTO, FeideLoginRequest } from '@/types'
import { UserRole } from '@/types'
import { useToast } from '@/composables/UseToast'
import { useStopStore } from '@/stores/StopStore'
import { useStoryProgressStore } from '@/stores/StoryProgressStore'
import { useSideQuestStore } from '@/stores/SideQuestStore'
import { useAudioStore } from '@/stores/AudioStore'

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

    function resetStopState(): void {
      const stop = useStopStore()
      stop.exitTestMode()
      stop.clearCompleted()
      stop.clearTasks()
      useStoryProgressStore().clear()
      useSideQuestStore().clear()
      useAudioStore().reset()
    }

    async function login(credentials: { email: string; password: string }): Promise<void> {
      const response = await AuthService.login(credentials)
      sessionStorage.setItem('auth', JSON.stringify({ token: response.token, refreshToken: response.refreshToken }))
      token.value = response.token
      refreshToken.value = response.refreshToken
      resetStopState()
      await fetchMe()
      await useStopStore().loadCompleted()
      await useStoryProgressStore().load()
      await useSideQuestStore().load()
    }

    async function feideLogin(credentials: FeideLoginRequest): Promise<void> {
      sessionStorage.removeItem('auth')
      token.value = null
      refreshToken.value = null
      user.value = null
      const response = await AuthService.feideLogin(credentials)
      sessionStorage.setItem('auth', JSON.stringify({ token: response.token, refreshToken: response.refreshToken }))
      token.value = response.token
      refreshToken.value = response.refreshToken
      resetStopState()
      await fetchMe()
      await useStopStore().loadCompleted()
      await useStoryProgressStore().load()
      await useSideQuestStore().load()
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

    function clearSession(): void {
      if (refreshToken.value) {
        AuthService.logout(refreshToken.value).catch(() => {})
      }
      sessionStorage.removeItem('auth')
      token.value = null
      refreshToken.value = null
      user.value = null
      resetStopState()
    }

    function logout(): void {
      if (refreshToken.value) {
        AuthService.logout(refreshToken.value).catch(() => {})
      }
      sessionStorage.removeItem('auth')
      token.value = null
      refreshToken.value = null
      user.value = null
      resetStopState()
      const { showSuccess } = useToast()
      showSuccess('Du er logget ut')
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
      login,
      feideLogin,
      fetchMe,
      refresh,
      clearSession,
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
