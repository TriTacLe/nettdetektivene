<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { School, Bell, Trophy } from 'lucide-vue-next'
import SceneComponent from "@/components/SceneComponent.vue"
import {
  getMyNotificationsOptions,
  getAllClassroomsOptions,
} from '@/generated-client/@tanstack/vue-query.gen'

const router = useRouter()

const { data: notificationsData } = useQuery({ ...getMyNotificationsOptions(), refetchInterval: 5000 })
const { data: classroomsData } = useQuery(getAllClassroomsOptions())

const unreadCount = computed(() =>
  (notificationsData.value?.data ?? []).filter((n) => !n.isRead).length
)

const classroomCount = computed(() =>
  (classroomsData.value?.data ?? []).length
)

const menuItems = [
  {
    title: 'Oversikt klasserom',
    subtitle: 'Se og administrer dine klasserom',
    route: 'classroom-overview',
    icon: School,
  },
  {
    title: 'Varsler',
    subtitle: 'Se alle varsler',
    route: 'teacher-notifications',
    icon: Bell,
  },
  {
    title: 'Ledertavler',
    subtitle: 'Se klassenes resultater',
    route: 'teacher-leaderboard',
    icon: Trophy,
  },
]
</script>

<template>
  <SceneComponent
    title="Lærerportal"
    background="/StartBackground.jpg">

    <div class="flex flex-col items-center justify-center flex-1 w-full px-6 pb-10">
      <nav class="flex flex-col w-full max-w-md mx-auto" aria-label="Lærerportal-navigasjon">
        <div class="flex flex-col gap-4">
          <button
            v-for="item in menuItems"
            :key="item.route"
            :aria-label="`${item.title} - ${item.subtitle}`"
            class="btn-bare menu-btn group relative w-full bg-white/95 backdrop-blur shadow-lg flex items-center gap-4 hover:shadow-xl hover:scale-[1.01] transition-all text-left focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2"
            @click="router.push({ name: item.route })"
          >
            <span class="text-stone-600 shrink-0" aria-hidden="true">
              <component :is="item.icon" :size="20" />
            </span>
            <div class="flex-1">
              <p class="font-bold text-gray-900 text-base leading-tight">{{ item.title }}</p>
              <p class="text-gray-500 text-sm mt-0.5">
                {{ item.subtitle }}
                <template v-if="item.route === 'classroom-overview' && classroomCount > 0">
                  ({{ classroomCount }})
                </template>
              </p>
            </div>
            <span
              v-if="item.route === 'teacher-notifications' && unreadCount > 0"
              class="absolute top-3 right-3 bg-red-500 text-white text-xs font-bold rounded-full min-w-[20px] h-5 flex items-center justify-center px-1.5"
              aria-hidden="true"
            >
              {{ unreadCount }}
            </span>
            <span class="text-gray-600 text-lg font-bold transition-transform group-hover:translate-x-1" aria-hidden="true">&rsaquo;</span>
          </button>
        </div>
      </nav>
    </div>
  </SceneComponent>
</template>

<style scoped>
.menu-btn {
  padding: 1rem 1.5rem 1rem 1rem;
  border: none;
  border-radius: 0;
  clip-path: var(--notch-clip);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  font-size: inherit;
  font-weight: inherit;
  animation: slideUp 0.7s ease-out both;
}

nav > div .menu-btn:nth-child(1) { animation-delay: 0.05s; }
nav > div .menu-btn:nth-child(2) { animation-delay: 0.1s; }
nav > div .menu-btn:nth-child(3) { animation-delay: 0.15s; }

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
