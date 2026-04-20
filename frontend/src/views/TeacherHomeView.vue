<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/AuthStore.ts'
import SceneComponent from "@/components/SceneComponent.vue";

const router = useRouter()
const authStore = useAuthStore()

function goTo(name: string) {
  router.push({ name })
}
</script>

<template>
  <SceneComponent background="/StartBackground.jpg">
    <div class="min-h-creen flex flex-col items-center justify-center p-8">
      <div class="text-center mb-10">
        <h1 class="text-fxl font-bold text-amber-900 drop-shadow">
          Nettdetektivenes lærerportal
        </h1>
      </div>

      <nav class="flex flex-col gap-4 w-full max-w-sm">
        <button
          @click="goTo('classroooms-overview')"
          class="w-full py-4 px-6 bg-amber-800 hover:bg-amber-700 text-white text-xl font-semibold rounded-xl shadow-lg transition"
        >
          Oversikt klasserom
        </button>

        <button
          @click="goTo('teacher-notifications')"
          class="w-full py-4 px-6 bg-amber-800 hover:bg-amber-700 text-white text-xl font-semibold rounded-xl shadow-lg transition"
        >
          Varslinger
        </button>

        <button
          @click="goTo('leaderboard')"
          class="w-full py-4 px-6 bg-amber-800 hover:bg-amber-700 text-white text-xl font-semibold rounded-xl shadow-lg transition"
        >
          Ledertavle
        </button>

        <button
          @click="goTo('settings')"
          class="w-full py-4 px-6 bg-amber-800 hover:bg-amber-700 text-white text-xl font-semibold rounded-xl shadow-lg transition"
        >
          Innstillinger
        </button>
      </nav>
    </div>
  </SceneComponent>
</template>
