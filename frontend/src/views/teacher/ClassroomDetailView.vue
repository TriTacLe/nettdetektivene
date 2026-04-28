<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery } from '@tanstack/vue-query'
import { getClassroomOptions, getMyNotificationsOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'

const route = useRoute()
const router = useRouter()
const classroomId = route.params.id as string

const { data } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroom = computed(() => data.value?.data ?? null)

const { data: notificationsData } = useQuery(getMyNotificationsOptions())
const unreadCount = computed(() =>
  (notificationsData.value?.data ?? []).filter((n) => !n.isRead && n.classroomId === classroomId).length
)

const menuItems = [
  {
    title: 'Oversikt Elever',
    subtitle: 'Se og administrer elever',
    route: 'classroom-students',
  },
  {
    title: 'Oversikt Oppgaver',
    subtitle: 'Oppgaver i spillet',
    route: 'classroom-tasks',
  },
  {
    title: 'Varsler',
    subtitle: 'Meldinger fra klassen',
    route: 'notifications',
  },
  {
    title: 'Klasseromsstyring',
    subtitle: 'Rediger innstillinger',
    route: 'classroom-settings',
  },
  {
    title: 'Test Spillet',
    subtitle: 'Spill som elev',
    route: 'classroom-test-game',
  },
]

function handleMenuItem(item: (typeof menuItems)[0]) {
  if (!item.route) return
  if (item.route === 'notifications') {
    router.push({ name: 'teacher-notifications', query: { classroomId } })
    return
  }
  router.push({ name: item.route, params: { id: classroomId } })
}
</script>

<template>
  <SceneComponent
    :title="classroom?.title || 'Klasserom'"
    previous="/classroom-overview"
    background="/StartBackground.jpg"
  >
    <div class="p-6 flex flex-col gap-3">
      <div class="text-center mb-3">
        <h2 class="text-white text-sm font-medium mb-1">Klasseromskode</h2>
        <p class="text-4xl font-mono font-black tracking-widest text-white">{{ classroom?.joinCode }}</p>
      </div>

      <div class="grid grid-cols-2 gap-3">
      <button
        v-for="item in menuItems"
        :key="item.title"
        :disabled="!item.route"
        class="relative bg-white rounded-xl p-4 flex items-center justify-between text-left shadow transition hover:brightness-95 disabled:opacity-50 disabled:cursor-not-allowed"
        @click="handleMenuItem(item)"
      >
        <div>
          <p class="font-bold text-gray-900 text-sm">{{ item.title }}</p>
          <p class="text-gray-500 text-xs mt-0.5">{{ item.subtitle }}</p>
        </div>
        <span
          v-if="item.route === 'notifications' && unreadCount > 0"
          class="absolute top-2 right-2 bg-red-500 text-white text-xs font-bold rounded-full min-w-[20px] h-5 flex items-center justify-center px-1.5"
        >
          {{ unreadCount }}
        </span>
        <span class="text-gray-400 text-lg">›</span>
      </button>
      </div>
    </div>
  </SceneComponent>
</template>
