<script setup lang="ts">
import { ref, computed, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { CheckCircle, Trophy, Clock, Search, AlertTriangle, UserPlus, Pencil, XCircle, Check } from 'lucide-vue-next'
import SceneComponent from '@/components/SceneComponent.vue'
import {
  getMyNotificationsOptions,
  getMyNotificationsQueryKey,
  getAllClassroomsOptions,
  markAsReadMutation,
  approveMutation,
  removeMutation,
  approveNameChangeMutation,
  rejectNameChangeMutation,
} from '@/generated-client/@tanstack/vue-query.gen'
import { NotificationType } from '@/types'
import type { NotificationResponseDto } from '@/generated-client/types.gen'

const route = useRoute()
const queryClient = useQueryClient()

const classroomId = computed(() => route.query.classroomId as string | undefined)
const backRoute = computed(() =>
  classroomId.value ? `/classroom/${classroomId.value}` : '/teacher-home'
)

const REF_TYPE_MAP: Record<string, NotificationType> = {
  mystery_submission: NotificationType.MYSTERY_SUBMITTED,
  stop_completed: NotificationType.STOP_COMPLETED,
  game_completed: NotificationType.CLASS_COMPLETED_GAME,
  stuck: NotificationType.STUCK,
  inappropriate_message: NotificationType.INAPPROPRIATE_MESSAGE,
  inappropriate_username: NotificationType.INAPPROPRIATE_USERNAME,
  classroom_student: NotificationType.JOIN_REQUEST,
  name_change: NotificationType.NAME_CHANGE,
  task_attempt_failed: NotificationType.FAILED,
  task_attempt_correct: NotificationType.CORRECT,
}

const toastMsg = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

function showToast(msg: string) {
  toastMsg.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastMsg.value = '' }, 3000)
}

const activeTypeFilter = ref<NotificationType | 'INAPPROPRIATE' | 'COMPLETED' | null>(null)
const activeClassroomFilter = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = 10

const { data, isLoading, error } = useQuery({ ...getMyNotificationsOptions(), refetchInterval: 5000 })
const { data: classroomsData } = useQuery(getAllClassroomsOptions())

const classroomNameMap = computed(() => {
  const map = new Map<string, string>()
  for (const c of classroomsData.value?.data ?? []) {
    if (c.id && c.title) map.set(c.id, c.title)
  }
  return map
})

const invalidateNotifications = () =>
  queryClient.invalidateQueries({ queryKey: getMyNotificationsQueryKey() })

const markRead = useMutation({
  ...markAsReadMutation(),
  onSuccess: invalidateNotifications,
})

const approveStudent = useMutation({
  ...approveMutation(),
  onSuccess: invalidateNotifications,
})

const removeStudent = useMutation({
  ...removeMutation(),
  onSuccess: invalidateNotifications,
})

const approveName = useMutation({
  ...approveNameChangeMutation(),
  onSuccess: () => { invalidateNotifications(); showToast('Navneendring godkjent') },
})

const rejectName = useMutation({
  ...rejectNameChangeMutation(),
  onSuccess: () => { invalidateNotifications(); showToast('Navneendring avvist') },
})

function handleApproveStudent(notification: { classroomId: string; refId: string; id: string }) {
  approveStudent.mutate({ path: { classroomId: notification.classroomId, userId: notification.refId } })
  markRead.mutate({ path: { id: notification.id } })
}

function handleRejectStudent(notification: { classroomId: string; refId: string; id: string }) {
  removeStudent.mutate({ path: { classroomId: notification.classroomId, userId: notification.refId } })
  markRead.mutate({ path: { id: notification.id } })
}

function handleApproveName(notification: { classroomId: string; refId: string; id: string }) {
  approveName.mutate({ path: { classroomId: notification.classroomId, userId: notification.refId } })
  markRead.mutate({ path: { id: notification.id } })
}

function handleRejectName(notification: { classroomId: string; refId: string; id: string }) {
  rejectName.mutate({ path: { classroomId: notification.classroomId, userId: notification.refId } })
  markRead.mutate({ path: { id: notification.id } })
}

const notifications = computed(() =>
  (data.value?.data ?? [])
    .map((raw: NotificationResponseDto) => ({
      ...raw,
      id: raw.id ?? '',
      classroomId: raw.classroomId ?? '',
      refId: raw.refId ?? '',
      msg: raw.msg ?? '',
      createdAt: raw.createdAt ?? '',
      isRead: raw.isRead ?? false,
      type: REF_TYPE_MAP[raw.refType ?? ''] ?? NotificationType.CORRECT,
    }))
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
)

const availableClassrooms = computed(() => {
  const seen = new Set<string>()
  return notifications.value
    .filter((n) => {
      if (seen.has(n.classroomId)) return false
      seen.add(n.classroomId)
      return true
    })
    .map((n) => ({ id: n.classroomId, name: classroomNameMap.value.get(n.classroomId) ?? n.classroomId }))
})

const allFiltered = computed(() => {
  let result = notifications.value
  if (classroomId.value) result = result.filter((n) => n.classroomId === classroomId.value)
  if (activeClassroomFilter.value) result = result.filter((n) => n.classroomId === activeClassroomFilter.value)
  if (activeTypeFilter.value === 'COMPLETED') {
    result = result.filter((n) =>
      n.type === NotificationType.STOP_COMPLETED || n.type === NotificationType.CLASS_COMPLETED_GAME
    )
  } else if (activeTypeFilter.value === 'INAPPROPRIATE') {
    result = result.filter((n) =>
      n.type === NotificationType.INAPPROPRIATE_MESSAGE || n.type === NotificationType.INAPPROPRIATE_USERNAME
    )
  } else if (activeTypeFilter.value) {
    result = result.filter((n) => n.type === activeTypeFilter.value)
  }
  return result
})

const totalPages = computed(() => Math.max(1, Math.ceil(allFiltered.value.length / pageSize)))

const filteredNotifications = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return allFiltered.value.slice(start, start + pageSize)
})

function setTypeFilter(type: NotificationType | 'INAPPROPRIATE' | 'COMPLETED' | null) {
  activeTypeFilter.value = type
  currentPage.value = 1
}

function setClassroomFilter(id: string | null) {
  activeClassroomFilter.value = id
  currentPage.value = 1
}

function setPage(page: number) {
  currentPage.value = page
}

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
  [NotificationType.JOIN_REQUEST]: {
    icon: UserPlus,
    iconColor: 'text-teal-600',
    border: 'border-l-teal-400',
    iconBg: 'bg-teal-100',
    label: 'Forespørsel om å bli med',
  },
  [NotificationType.NAME_CHANGE]: {
    icon: Pencil,
    iconColor: 'text-orange-600',
    border: 'border-l-orange-400',
    iconBg: 'bg-orange-100',
    label: 'Navneendring',
  },
  [NotificationType.FAILED]: {
    icon: XCircle,
    iconColor: 'text-red-600',
    border: 'border-l-red-400',
    iconBg: 'bg-red-100',
    label: 'Feilet',
  },
  [NotificationType.CORRECT]: {
    icon: Check,
    iconColor: 'text-emerald-600',
    border: 'border-l-emerald-400',
    iconBg: 'bg-emerald-100',
    label: 'Riktig',
  },
}

function formatTime(iso: string): string {
  // Backend serializes LocalDateTime without a timezone (e.g. "2026-04-28T14:39:05").
  // The DB and JVM run in UTC, so treat unmarked timestamps as UTC; otherwise the
  // browser would parse them as local time and add a fixed offset to every value.
  const hasZone = /Z$|[+-]\d{2}:?\d{2}$/.test(iso)
  const date = new Date(hasZone ? iso : iso + 'Z')
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
    <transition name="toast">
      <div
        v-if="toastMsg"
        class="fixed top-6 left-1/2 -translate-x-1/2 z-50 px-6 py-3 rounded-2xl bg-green-600 text-white font-semibold shadow-xl text-sm"
      >
        {{ toastMsg }}
      </div>
    </transition>

    <div class="overflow-y-auto flex-1 px-6 pb-10 w-full max-w-2xl mx-auto">

      <div class="flex gap-3 flex-wrap mb-4 mt-2">
        <select
          @change="setTypeFilter(($event.target as HTMLSelectElement).value === 'all' ? null : ($event.target as HTMLSelectElement).value as any)"
          :value="activeTypeFilter ?? 'all'"
          class="text-sm px-3 py-1.5 rounded-xl border border-white/20 bg-white/10 text-white font-medium backdrop-blur-sm"
        >
          <option value="all" class="text-gray-900">Alle varsler</option>
          <option value="COMPLETED" class="text-gray-900">Fullført</option>
          <option :value="NotificationType.STUCK" class="text-gray-900">Elev står fast</option>
          <option :value="NotificationType.MYSTERY_SUBMITTED" class="text-gray-900">Ukens mysterium</option>
          <option value="INAPPROPRIATE" class="text-gray-900">Upassende innhold</option>
          <option :value="NotificationType.JOIN_REQUEST" class="text-gray-900">Forespørsler</option>
          <option :value="NotificationType.NAME_CHANGE" class="text-gray-900">Navneendring</option>
          <option :value="NotificationType.FAILED" class="text-gray-900">Feilet</option>
          <option :value="NotificationType.CORRECT" class="text-gray-900">Riktig</option>
        </select>

        <select
          v-if="!classroomId"
          @change="setClassroomFilter(($event.target as HTMLSelectElement).value === 'all' ? null : ($event.target as HTMLSelectElement).value)"
          :value="activeClassroomFilter ?? 'all'"
          class="text-sm px-3 py-1.5 rounded-xl border border-white/20 bg-white/10 text-white font-medium backdrop-blur-sm"
        >
          <option value="all" class="text-gray-900">Alle klasser</option>
          <option v-for="classroom in availableClassrooms" :key="classroom.id" :value="classroom.id" class="text-gray-900">
            {{ classroom.name }}
          </option>
        </select>
      </div>

      <div v-if="isLoading" class="text-white text-center py-16 text-lg">
        Laster varsler...
      </div>

      <div
        v-else-if="error"
        class="bg-white rounded-2xl shadow px-5 py-4 text-red-700 font-medium"
      >
        Kunne ikke hente varsler
      </div>

      <div
        v-else-if="filteredNotifications.length === 0"
        class="text-white/80 text-center py-16 text-lg"
      >
        Ingen varsler å vise.
      </div>

      <ul v-else class="flex flex-col gap-3">
        <li
          v-for="notification in filteredNotifications"
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
              <span class="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
                {{ TYPE_STYLES[notification.type].label }}
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

            <div
              v-if="!notification.isRead && notification.type === NotificationType.JOIN_REQUEST"
              class="flex gap-2 mt-2"
            >
              <button
                @click.stop="handleApproveStudent(notification)"
                class="text-xs font-bold text-green-700 hover:text-green-500 bg-green-50 px-3 py-1 rounded-lg"
              >
                Godkjenn
              </button>
              <button
                @click.stop="handleRejectStudent(notification)"
                class="text-xs font-bold text-red-600 hover:text-red-800 bg-red-50 px-3 py-1 rounded-lg"
              >
                Avvis
              </button>
            </div>

            <div
              v-if="!notification.isRead && notification.type === NotificationType.NAME_CHANGE"
              class="flex gap-2 mt-2"
            >
              <button
                @click.stop="handleApproveName(notification)"
                class="text-xs font-bold text-green-700 hover:text-green-500 bg-green-50 px-3 py-1 rounded-lg"
              >
                Godkjenn
              </button>
              <button
                @click.stop="handleRejectName(notification)"
                class="text-xs font-bold text-red-600 hover:text-red-800 bg-red-50 px-3 py-1 rounded-lg"
              >
                Avvis
              </button>
            </div>
          </div>

          <span
            v-if="!notification.isRead"
            role="button"
            tabindex="0"
            @click.stop="markRead.mutate({ path: { id: notification.id } })"
            @keydown.enter.stop.prevent="markRead.mutate({ path: { id: notification.id } })"
            @keydown.space.stop.prevent="markRead.mutate({ path: { id: notification.id } })"
            class="self-start shrink-0 mt-0.5 px-2 py-1 text-[10px] leading-none font-semibold uppercase tracking-wide text-gray-400 hover:text-gray-700 cursor-pointer select-none transition rounded"
          >
            Dempe
          </span>
        </li>
      </ul>

      <div v-if="totalPages > 1" class="flex items-center justify-center gap-3 mt-6 bg-white/20 backdrop-blur-sm rounded-2xl px-5 py-3 w-fit mx-auto">
        <button
          :disabled="currentPage === 1"
          style="all: unset; cursor: pointer; font-weight: bold; color: white; font-size: 1rem;"
          :style="currentPage === 1 ? 'opacity: 0.3; cursor: not-allowed;' : ''"
          @click="setPage(currentPage - 1)"
        >
          ←
        </button>
        <span class="text-white text-sm font-medium px-3">
          Side {{ currentPage }} av {{ totalPages }}
        </span>
        <button
          :disabled="currentPage === totalPages"
          style="all: unset; cursor: pointer; font-weight: bold; color: white; font-size: 1rem;"
          :style="currentPage === totalPages ? 'opacity: 0.3; cursor: not-allowed;' : ''"
          @click="setPage(currentPage + 1)"
        >
          →
        </button>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-8px);
}
</style>
