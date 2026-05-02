<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/AuthStore.ts'
import { Settings } from 'lucide-vue-next'
import SettingsModal from './SettingsModal.vue'

defineProps<{
  previous?: string
  title: string
  next?: string
}>()

const emit = defineEmits<{ back: [] }>()
const router = useRouter()
const authStore = useAuthStore()
const settingsOpen = ref(false)

let nameRefreshInterval: ReturnType<typeof setInterval> | null = null

function goTo(path?: string) {
  if (path) {
    router.push(path)
  } else {
    emit('back')
  }
}

onMounted(() => {
  if (!authStore.isTeacher && authStore.user) {
    nameRefreshInterval = setInterval(() => {
      if (!authStore.isTeacher && authStore.user) {
        authStore.fetchMe().catch(() => {})
      }
    }, 15000)
  }
})

onUnmounted(() => {
  if (nameRefreshInterval !== null) {
    clearInterval(nameRefreshInterval)
    nameRefreshInterval = null
  }
})
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
      <span v-if="!authStore.isTeacher && authStore.user" class="nav-user">
        {{ authStore.user.displayName }}
      </span>
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
      <button
        v-if="authStore.isAuthenticated"
        class="nav-button settings-button btn-bare"
        aria-label="Åpne innstillinger"
        @click="settingsOpen = true">
        <Settings :size="34" color="white" />
        <span class="settings-label">Innstillinger</span>
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
  font-size: 1.5rem;
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
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-right: 16px;
  padding-top: 8px;
}

.settings-label {
  font-size: 1rem;
  font-weight: 600;
  color: white;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
}

.nav-title {
  flex: 1;
  text-align: left;
  font-size: 2.6rem;
  font-weight: 400;
  color: white;
  margin: 0 16px 0 42px;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  letter-spacing: 0.03em;
  -webkit-text-stroke: 0;
  paint-order: normal;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.6);
  padding-top: 10px;
}

@media (max-width: 768px) {
  .navigation-header {
    padding: 12px 16px 12px 8px;
    overflow: hidden;
  }

  .nav-title {
    font-size: 1.6rem;
    margin: 0 8px 0 12px;
    padding-top: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }

  .nav-right {
    flex-shrink: 1;
  }

  .nav-user {
    display: none;
  }

  .settings-label {
    display: none;
  }

  .settings-button {
    margin-right: 4px;
  }
}
</style>
