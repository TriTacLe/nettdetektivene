<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { School, ClipboardList, Bell, Settings, Trophy } from 'lucide-vue-next'
import SceneComponent from "@/components/SceneComponent.vue"
import {
  getMyNotificationsOptions,
  getAllClassroomsOptions,
} from '@/generated-client/@tanstack/vue-query.gen'

const router = useRouter()

const { data: notificationsData } = useQuery(getMyNotificationsOptions())
const { data: classroomsData } = useQuery(getAllClassroomsOptions())

const unreadCount = computed(() =>
  (notificationsData.value?.data ?? []).filter((n) => !n.isRead).length
)

const classroomCount = computed(() =>
  (classroomsData.value?.data ?? []).length
)

const primaryItems = [
  {
    title: 'Oversikt klasserom',
    subtitle: 'Se og administrer dine klasserom',
    route: 'classroom-overview',
    icon: School,
  },
  {
    title: 'Oppgaver',
    subtitle: 'Se alle oppgaver',
    route: 'tasks',
    icon: ClipboardList,
  },
  {
    title: 'Varsler',
    subtitle: 'Se alle varsler',
    route: 'teacher-notifications',
    icon: Bell,
  },
]

const secondaryItems = [
  {
    title: 'Ledertavler',
    subtitle: 'Se klassenes resultater',
    route: 'teacher-leaderboard',
    icon: Trophy,
  },
  {
    title: 'Innstillinger',
    subtitle: 'Konto og lydinnstillinger',
    route: 'settings',
    icon: Settings,
  },
]

</script>

<template>
  <SceneComponent
    title="Lærerportal"
    previous="/teacher-login"
    background="/StartBackground.jpg"
    dim-overlay>

    <div class="flex flex-col items-center justify-center flex-1 w-full px-6 pb-10">
      <nav class="flex flex-col w-full max-w-md mx-auto" aria-label="Lærerportal-navigasjon">
        <div class="flex flex-col gap-4">
          <button
            v-for="item in primaryItems"
            :key="item.route"
            :aria-label="`${item.title} - ${item.subtitle}`"
            class="menu-btn group relative w-full bg-white/95 backdrop-blur rounded-2xl shadow-lg flex items-center gap-4 hover:shadow-xl hover:scale-[1.01] transition-all text-left focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2"
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

        <div class="flex items-center gap-3 my-4" aria-hidden="true">
          <div class="flex-1 h-px bg-white/20" />
          <span class="text-white/40 text-xs uppercase tracking-widest">Mer</span>
          <div class="flex-1 h-px bg-white/20" />
        </div>

        <div class="flex flex-col gap-4">
          <button
            v-for="item in secondaryItems"
            :key="item.route"
            :aria-label="`${item.title} - ${item.subtitle}`"
            class="menu-btn group relative w-full bg-white/90 backdrop-blur rounded-2xl shadow-md flex items-center gap-4 hover:shadow-lg hover:scale-[1.01] transition-all text-left focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2"
            @click="router.push({ name: item.route })"
          >
            <span class="text-stone-500 shrink-0" aria-hidden="true">
              <component :is="item.icon" :size="20" />
            </span>
            <div class="flex-1">
              <p class="font-bold text-gray-800 text-sm leading-tight">{{ item.title }}</p>
              <p class="text-gray-400 text-xs mt-0.5">{{ item.subtitle }}</p>
            </div>
            <span class="text-gray-500 text-lg font-bold transition-transform group-hover:translate-x-1" aria-hidden="true">&rsaquo;</span>
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
  font-size: inherit;
  font-weight: inherit;
  animation: slideUp 0.7s ease-out both;
}

nav > div:first-child .menu-btn:nth-child(1) { animation-delay: 0.05s; }
nav > div:first-child .menu-btn:nth-child(2) { animation-delay: 0.1s; }
nav > div:first-child .menu-btn:nth-child(3) { animation-delay: 0.15s; }
nav > div:last-child .menu-btn:nth-child(1) { animation-delay: 0.25s; }
nav > div:last-child .menu-btn:nth-child(2) { animation-delay: 0.3s; }

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
