<script setup lang="ts">
import { computed, onMounted, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { CheckCircle, Trophy, Clock, Search, AlertTriangle } from 'lucide-vue-next'
import SceneComponent from '@/components/SceneComponent.vue'
import { useNotificationStore } from '@/stores/NotificationStore'
import { NotificationType } from '@/types'

const route = useRoute()
const store = useNotificationStore()

const classroomId = computed(() => route.query.classroomId as string | undefined)

const backRoute = computed(() =>
  classroomId.value ? `/classroom/${classroomId.value}` : '/teacher-home'
)

onMounted(async () => {
  await store.fetchNotifications(classroomId.value)
})

interface NotificationStyle {
  icon: Component
  iconColor: string
  border: string
  iconBg: string
  label: string
}

const TYPE_STYLES: Record<NotificationType, NotificationStyle> = {
  [NotificationType.STOP_COMPLETED]: {
    icon: CheckCircle,
    iconColor: 'text-green-600',
    border: 'border-l-green-400',
    iconBg: 'bg-green-100',
    label: 'Område fullført',
  },
  [NotificationType.CLASS_COMPLETED_GAME]: {
    icon: Trophy,
    iconColor: 'text-purple-600',
    border: 'border-l-purple-400',
    iconBg: 'bg-purple-100',
    label: 'Klassen fullførte spillet',
  },
  [NotificationType.STUCK]: {
    icon: Clock,
    iconColor: 'text-amber-600',
    border: 'border-l-amber-400',
    iconBg: 'bg-amber-100',
    label: 'Elev står fast',
  },
  [NotificationType.MYSTERY_SUBMITTED]: {
    icon: Search,
    iconColor: 'text-blue-600',
    border: 'border-l-blue-400',
    iconBg: 'bg-blue-100',
    label: 'Ukens mysterium',
  },
  [NotificationType.INAPPROPRIATE_MESSAGE]: {
    icon: AlertTriangle,
    iconColor: 'text-red-600',
    border: 'border-l-red-400',
    iconBg: 'bg-red-100',
    label: 'Upassende tekst',
  },
  [NotificationType.INAPPROPRIATE_USERNAME]: {
    icon: AlertTriangle,
    iconColor: 'text-red-600',
    border: 'border-l-red-400',
    iconBg: 'bg-red-100',
    label: 'Upassende brukernavn',
  },
}

function formatTime(iso: string): string {
  const date = new Date(iso)
  const now = Date.now()
  const diffMin = Math.floor((now - date.getTime()) / 60000)
  if (diffMin < 1) return 'Nå nettopp'
  if (diffMin < 60) return `${diffMin} min siden`
  if (diffMin < 1440) return `${Math.floor(diffMin / 60)} t siden`
  return date.toLocaleDateString('nb-NO')
}
</script>

<template>
  <SceneComponent title="Varsler" background="/StartBackground.jpg" :previous="backRoute">
    <div class="overflow-y-auto flex-1 px-6 pb-10 w-full max-w-2xl mx-auto">

      <div class="flex gap-3 flex-wrap mb-4 mt-2">
        <select
          @change="store.setTypeFilter(($event.target as HTMLSelectElement).value === 'all' ? null : ($event.target as HTMLSelectElement).value as any)"
          :value="store.activeTypeFilter ?? 'all'"
          class="text-sm px-3 py-1.5 rounded-xl border border-white/20 bg-white/10 text-white font-medium backdrop-blur-sm"
        >
          <option value="all" class="text-gray-900">Alle varsler</option>
          <option value="COMPLETED" class="text-gray-900">Fullført</option>
          <option :value="NotificationType.STUCK" class="text-gray-900">Elev står fast</option>
          <option :value="NotificationType.MYSTERY_SUBMITTED" class="text-gray-900">Ukens mysterium</option>
          <option value="INAPPROPRIATE" class="text-gray-900">Upassende innhold</option>
        </select>

        <select
          v-if="!classroomId"
          @change="store.setClassroomFilter(($event.target as HTMLSelectElement).value === 'all' ? null : ($event.target as HTMLSelectElement).value)"
          :value="store.activeClassroomFilter ?? 'all'"
          class="text-sm px-3 py-1.5 rounded-xl border border-white/20 bg-white/10 text-white font-medium backdrop-blur-sm"
        >
          <option value="all" class="text-gray-900">Alle klasser</option>
          <option v-for="classroom in store.availableClassrooms" :key="classroom.id" :value="classroom.id" class="text-gray-900">
            {{ classroom.name }}
          </option>
        </select>
      </div>

      <div v-if="store.isLoading" class="text-white text-center py-16 text-lg">
        Laster varsler...
      </div>

      <div
        v-else-if="store.error"
        class="bg-white rounded-2xl shadow px-5 py-4 text-red-700 font-medium"
      >
        {{ store.error }}
      </div>

      <div
        v-else-if="store.filteredNotifications.length === 0"
        class="text-white/80 text-center py-16 text-lg"
      >
        Ingen varsler å vise.
      </div>

      <ul v-else class="flex flex-col gap-3">
        <li
          v-for="notification in store.filteredNotifications"
          :key="notification.id"
          :class="[
            TYPE_STYLES[notification.type].border,
            'bg-white rounded-2xl shadow px-5 py-4 flex items-start gap-4 border-l-4 transition',
            notification.isRead ? 'opacity-70' : 'opacity-100',
          ]"
        >
          <div :class="['rounded-full p-2 shrink-0', TYPE_STYLES[notification.type].iconBg]">
            <component
              :is="TYPE_STYLES[notification.type].icon"
              :class="TYPE_STYLES[notification.type].iconColor"
              :size="18"
              :stroke-width="2"
            />
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="font-bold text-gray-900 text-sm leading-tight">
                {{ notification.studentName || notification.classroomName }}
              </span>
              <span class="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
                {{ TYPE_STYLES[notification.type].label }}
              </span>
              <span
                v-if="!classroomId"
                class="text-xs text-gray-400 bg-gray-50 px-2 py-0.5 rounded-full border border-gray-200"
              >
                {{ notification.classroomName }}
              </span>
              <span
                v-if="!notification.isRead"
                class="text-xs text-white bg-gray-700 px-2 py-0.5 rounded-full font-semibold"
              >
                Ny
              </span>
            </div>
            <p class="text-gray-700 mt-1 text-sm">{{ notification.msg }}</p>
            <p class="text-xs text-gray-400 mt-1">{{ formatTime(notification.createdAt) }}</p>
          </div>

          <button
            v-if="!notification.isRead"
            @click.stop="store.markAsRead(notification.id)"
            style="all: unset; cursor: pointer;"
            class="text-[10px] leading-none text-gray-400 hover:text-gray-600 shrink-0 mt-0.5 transition"
          >
            Dempe
          </button>
        </li>
      </ul>

      <div v-if="store.totalPages > 1" class="flex items-center justify-center gap-3 mt-6 bg-white/20 backdrop-blur-sm rounded-2xl px-5 py-3 w-fit mx-auto">
        <button
          :disabled="store.currentPage === 1"
          class="text-white font-bold text-base transition disabled:opacity-30 disabled:cursor-not-allowed hover:text-white/70"
          style="all: unset; cursor: pointer; font-weight: bold; color: white; font-size: 1rem;"
          :style="store.currentPage === 1 ? 'opacity: 0.3; cursor: not-allowed;' : ''"
          @click="store.setPage(store.currentPage - 1)"
        >
          ←
        </button>
        <span class="text-white text-sm font-medium px-3">
          Side {{ store.currentPage }} av {{ store.totalPages }}
        </span>
        <button
          :disabled="store.currentPage === store.totalPages"
          style="all: unset; cursor: pointer; font-weight: bold; color: white; font-size: 1rem;"
          :style="store.currentPage === store.totalPages ? 'opacity: 0.3; cursor: not-allowed;' : ''"
          @click="store.setPage(store.currentPage + 1)"
        >
          →
        </button>
      </div>
    </div>
  </SceneComponent>
</template>
