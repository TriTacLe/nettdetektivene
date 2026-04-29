<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import SceneComponent from "@/components/SceneComponent.vue"
import { getMyNotificationsOptions } from '@/generated-client/@tanstack/vue-query.gen'

const router = useRouter()

const { data: notificationsData } = useQuery({ ...getMyNotificationsOptions(), refetchInterval: 5000 })

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
    background="/StartBackground.jpg">

    <div class="flex flex-col items-center justify-center flex-1 w-full px-6 pb-10">
      <nav class="menu-stack">
        <button
          v-for="item in menuItems"
          :key="item.route"
          class="btn-vn menu-item"
          @click="router.push({ name: item.route })"
        >
          <span class="menu-text">
            <span class="menu-title">{{ item.title }}</span>
            <span class="menu-subtitle">{{ item.subtitle }}</span>
          </span>
          <span
            v-if="item.route === 'teacher-notifications' && unreadCount > 0"
            class="menu-badge"
          >
            {{ unreadCount }}
          </span>
          <span class="menu-arrow" aria-hidden="true">›</span>
        </button>
      </nav>
    </div>
  </SceneComponent>
</template>
