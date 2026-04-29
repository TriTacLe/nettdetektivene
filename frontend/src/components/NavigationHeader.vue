<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { useAuthStore } from '@/stores/AuthStore.ts'
import { getMyNotificationsOptions } from '@/generated-client/@tanstack/vue-query.gen'
import SettingsModal from './SettingsModal.vue'

defineProps<{
  previous?: string
  title: string
  next?: string
}>()

const emit = defineEmits<{ back: [] }>()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const settingsOpen = ref(false)

const NO_SETTINGS_ROUTES = new Set(['start', 'student-login', 'teacher-login', 'join-classroom', 'set-display-name'])
const showSettings = computed(() => !NO_SETTINGS_ROUTES.has(route.name as string))

const PRE_CLASSROOM_ROUTES = new Set(['join-classroom', 'set-display-name', 'awaiting-approval'])
const showLogout = computed(() => !!authStore.user && !authStore.isTeacher && PRE_CLASSROOM_ROUTES.has(route.name as string))

const notificationOpts = getMyNotificationsOptions()
const { data: notificationsData } = useQuery({
  ...notificationOpts,
  queryKey: notificationOpts.queryKey as any,
  enabled: computed(() => authStore.isTeacher && !NO_SETTINGS_ROUTES.has(route.name as string)),
})

const unreadCount = computed(() =>
  (notificationsData.value?.data ?? []).filter((n: { isRead?: boolean }) => !n.isRead).length
)

function logout() {
  authStore.logout()
  router.push({ name: 'student-login' })
}

function goTo(path?: string) {
  if (path) {
    router.push(path)
  } else {
    emit('back')
  }
}
</script>

<template>
  <header class="navigation-header">
    <div v-if="previous" class="nav-previous" @click="goTo()">
      <button class="nav-button btn-bare" aria-label="Go back">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="12" fill="white" />
          <path
            d="M14 8L10 12L14 16"
            stroke="black"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
    </div>
    <div v-else class="nav-placeholder" />

    <h1 class="nav-title">{{ title }}</h1>

    <div class="nav-right">
      <button
        v-if="authStore.isTeacher && showSettings"
        class="nav-bell btn-bare"
        :aria-label="unreadCount > 0 ? `${unreadCount} uleste varsler` : 'Ingen uleste varsler'"
        @click="router.push({ name: 'teacher-notifications' })"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        <span v-if="unreadCount > 0" class="nav-bell-badge">{{ unreadCount }}</span>
      </button>
      <span v-if="!authStore.isTeacher && authStore.user && showSettings" class="nav-user">
        {{ authStore.user.displayName }}
      </span>
      <button
        v-if="showLogout"
        class="nav-button btn-bare logout-button"
        @click="logout"
      >
        Logg ut
      </button>
      <div v-if="next" class="nav-next" @click="goTo(next!)">
        <button class="nav-button btn-bare" aria-label="Go forward">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="12" fill="white" />
            <path
              d="M10 8L14 12L10 16"
              stroke="black"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
      </div>
      <div v-else class="nav-placeholder" />
      <button v-if="showSettings" class="nav-button settings-button btn-bare" aria-label="Apne innstillinger" @click="settingsOpen = true">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="12" fill="white" />
          <path
            d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.325.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 0 1 1.37.49l1.296 2.247a1.125 1.125 0 0 1-.26 1.431l-1.003.827c-.293.241-.438.613-.43.992a7.723 7.723 0 0 1 0 .255c-.008.378.137.75.43.991l1.004.827c.424.35.534.955.26 1.43l-1.298 2.247a1.125 1.125 0 0 1-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.47 6.47 0 0 1-.22.128c-.331.183-.581.495-.644.869l-.213 1.281c-.09.543-.56.94-1.11.94h-2.594c-.55 0-1.019-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 0 1-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 0 1-1.369-.49l-1.297-2.247a1.125 1.125 0 0 1 .26-1.431l1.004-.827c.292-.24.437-.613.43-.991a6.932 6.932 0 0 1 0-.255c.007-.38-.138-.751-.43-.992l-1.004-.827a1.125 1.125 0 0 1-.26-1.43l1.297-2.247a1.125 1.125 0 0 1 1.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.086.22-.128.332-.183.582-.495.644-.869l.214-1.28Z"
            fill="#888"
            stroke="black"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"
            fill="white"
            stroke="black"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
    </div>

    <SettingsModal v-if="settingsOpen" @close="settingsOpen = false" />
  </header>
</template>

<style scoped>
.navigation-header {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 16px 24px;
  background: transparent;
  box-sizing: border-box;
}

.nav-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-button:hover {
  opacity: 0.8;
}

.nav-placeholder {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.nav-previous {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.nav-previous:hover {
  opacity: 0.8;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.nav-user {
  font-size: 0.875rem;
  font-weight: 600;
  color: white;
}

.nav-next {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.nav-next:hover {
  opacity: 0.8;
}

.settings-button {
  flex-shrink: 0;
  flex-direction: column;
  gap: 2px;
}

.nav-bell {
  position: relative;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-bell:hover {
  opacity: 0.8;
}

.nav-bell-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  background: #ef4444;
  color: white;
  font-size: 0.65rem;
  font-weight: 700;
  border-radius: 9999px;
  min-width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.nav-title {
  flex: 1;
  text-align: left;
  font-size: 2rem;
  font-weight: 700;
  color: white;
  margin: 0 16px 0 42px;
}

.logout-button {
  font-size: 0.8rem;
  font-weight: 600;
  color: white;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 0.4rem;
  padding: 0.25rem 0.6rem;
  cursor: pointer;
}

.logout-button:hover {
  background: rgba(0, 0, 0, 0.55);
}
</style>
