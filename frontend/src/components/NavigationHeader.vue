<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/AuthStore.ts'
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
        class="nav-button settings-button btn-bare"
        aria-label="Apne innstillinger"
        @click="settingsOpen = true"
      >
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
  flex-direction: column;
  gap: 2px;
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

@media (max-width: 500px) {
  .nav-title {
    font-size: 1.6rem;
    margin: 0 8px 0 12px;
    padding-top: 0;
  }

  .nav-user {
    font-size: 0.75rem;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
