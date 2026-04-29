<script setup lang="ts">
import SceneComponent from '@/components/SceneComponent.vue'
import { useAuthStore } from '@/stores/AuthStore.ts'

const authStore = useAuthStore()

const menuItems = [
  {
    title: 'Logg ut',
    subtitle: 'Avslutt økten',
    action: () => authStore.logout(),
    danger: true,
  },
]
</script>

<template>
  <SceneComponent title="Innstillinger" previous="/teacher-home" background="/StartBackground.jpg">
    <div class="flex flex-col items-center flex-1 w-full px-6 pb-10 pt-4">
      <div class="menu-stack">
        <button
          v-for="item in menuItems"
          :key="item.title"
          class="btn-vn menu-item"
          :class="{ 'btn-vn-danger': item.danger }"
          @click="item.action()"
        >
          <span class="menu-text">
            <span class="menu-title">{{ item.title }}</span>
            <span class="menu-subtitle">{{ item.subtitle }}</span>
          </span>
          <span class="menu-arrow" aria-hidden="true">›</span>
        </button>
      </div>
    </div>
  </SceneComponent>
</template>
