<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import SceneComponent from "@/components/SceneComponent.vue"
import { getMyNotificationsOptions } from '@/generated-client/@tanstack/vue-query.gen'

const router = useRouter()

const { data: notificationsData } = useQuery(getMyNotificationsOptions())

const unreadCount = computed(() =>
  (notificationsData.value?.data ?? []).filter((n) => !n.isRead).length
)

const menuItems = [
  {
    title: 'Oversikt klasserom',
    subtitle: 'Se og administrer klasserom',
    route: 'classroom-overview',
  },
  {
    title: 'Varsler',
    subtitle: 'Se alle varsler',
    route: 'teacher-notifications',
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
          class="relative w-full bg-white rounded-2xl shadow px-5 py-4 flex items-center gap-4 hover:bg-gray-50 transition text-left"
        >
          <div class="flex-1">
            <p class="font-bold text-gray-900 text-base leading-tight">{{ item.title }}</p>
            <p class="text-gray-500 text-sm mt-0.5">{{ item.subtitle }}</p>
          </div>
          <span
            v-if="item.route === 'teacher-notifications' && unreadCount > 0"
            class="absolute top-3 right-3 bg-red-500 text-white text-xs font-bold rounded-full min-w-[20px] h-5 flex items-center justify-center px-1.5"
          >
            {{ unreadCount }}
          </span>
          <span class="text-gray-400 text-lg">›</span>
        </button>
      </nav>
    </div>
  </SceneComponent>
</template>
