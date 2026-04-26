<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/AuthStore.ts'

defineProps<{
  previous?: string
  title: string
  next?: string
}>()

const emit = defineEmits<{ back: [] }>()
const router = useRouter()
const authStore = useAuthStore()

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
      <button class="nav-button" aria-label="Go back">
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
      <span v-if="authStore.isTeacher && authStore.user" class="nav-user">
        {{ authStore.user.displayName ?? authStore.user.email }}
      </span>
      <div v-if="next" class="nav-next" @click="goTo(next!)">
        <button class="nav-button" aria-label="Go forward">
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
    </div>
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
  font-size: 0.8rem;
  font-weight: 600;
  color: white;
  opacity: 0.8;
}

.nav-next {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.nav-next:hover {
  opacity: 0.8;
}

.nav-title {
  flex: 1;
  text-align: left;
  font-size: 2rem;
  font-weight: 700;
  color: white;
  margin: 0 16px 0 42px;
}
</style>
