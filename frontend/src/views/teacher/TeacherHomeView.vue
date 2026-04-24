<script setup lang="ts">
import { useRouter } from 'vue-router'
import SceneComponent from "@/components/SceneComponent.vue";

const router = useRouter()

const menuItems = [
  {
    title: 'Oversikt klasserom',
    subtitle: 'Se og administrer klasserom',
    route: 'classroom-overview',
  },
  {
    title: 'Oppgaver',
    subtitle: 'Se alle oppgaver',
    route: 'tasks',
  },
  {
    title: 'Varsler',
    subtitle: 'Se alle varsler',
    route: 'teacher-notifications',
  },
  {
    title: 'Innstillinger',
    subtitle: 'Konto og lydinnstillinger',
    route: 'settings',
  },
  {
    title: 'Ledertavle',
    subtitle: 'Se klassenes resultater',
    route: 'teacher-leaderboard',
  },
]

</script>

<template>
  <SceneComponent
    title="Lærerportal"
    previous="/teacher-login"
    background="/StartBackground.jpg">

    <div class="flex flex-col items-center justify-center flex-1 w-full px-6 pb-10">
      <nav class="flex flex-col gap-3 w-full max-w-md mx-auto">
        <button
          v-for="item in menuItems"
          :key="item.route"
          @click="router.push({ name: item.route })"
          class="w-full bg-white rounded-2xl shadow px-5 py-4 flex items-center gap-4 hover:bg-gray-50 transition text-left"
        >
          <div class="flex-1">
            <p class="font-bold text-gray-900 text-base leading-tight">{{ item.title }}</p>
            <p class="text-gray-500 text-sm mt-0.5">{{ item.subtitle }}</p>
          </div>
          <span class="text-gray-400 text-lg">›</span>
        </button>
      </nav>
    </div>
  </SceneComponent>
</template>
