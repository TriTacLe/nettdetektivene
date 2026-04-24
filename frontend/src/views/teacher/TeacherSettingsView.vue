<script setup lang="ts">
import { useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useAuthStore } from '@/stores/AuthStore.ts'

const router = useRouter()
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
      <div class="flex flex-col gap-3 w-full max-w-md mx-auto">
        <button
          v-for="item in menuItems"
          :key="item.title"
          @click="item.action()"
          class="w-full bg-white rounded-2xl shadow px-5 py-4 flex items-center gap-4 hover:bg-red-50 transition text-left"
        >
          <div class="flex-1">
            <p class="font-bold text-base leading-tight text-red-700">{{ item.title }}</p>
            <p class="text-gray-500 text-sm mt-0.5">{{ item.subtitle }}</p>
          </div>
          <span class="text-gray-400 text-lg">›</span>
        </button>
      </div>
    </div>
  </SceneComponent>
</template>
