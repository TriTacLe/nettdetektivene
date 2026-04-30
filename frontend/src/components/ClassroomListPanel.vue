<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/vue-query'
import { Users, Plus, Trash2 } from 'lucide-vue-next'
import {
  getMyNotificationsOptions,
  getAllClassroomsOptions,
  getAllClassroomsQueryKey,
  getApprovedStudentsOptions,
  deleteClassroomMutation,
} from '@/generated-client/@tanstack/vue-query.gen.ts'

const router = useRouter()
const queryClient = useQueryClient()

const { data: classroomsData, isLoading, error } = useQuery(getAllClassroomsOptions())
const { data: notificationsData } = useQuery({ ...getMyNotificationsOptions(), refetchInterval: 5000 })

const classrooms = computed(() => classroomsData.value?.data ?? [])

const studentQueries = useQueries({
  queries: computed(() =>
    classrooms.value.map((c) =>
      getApprovedStudentsOptions({ path: { classroomId: c.id! } })
    )
  ),
})

function studentCount(index: number) {
  return studentQueries.value[index]?.data?.data?.length ?? 0
}

function classroomNotifications(classroomId: string) {
  return (notificationsData.value?.data ?? []).filter(
    (n) => !n.isRead && n.classroomId === classroomId
  ).length
}

const deleteMutation = useMutation({
  ...deleteClassroomMutation(),
  onSuccess: () => queryClient.invalidateQueries({ queryKey: getAllClassroomsQueryKey() }),
})

function deleteClassroom(id: string) {
  if (!confirm('Er du sikker på at du vil slette dette klasserommet?')) return
  deleteMutation.mutate({ path: { id } })
}
</script>

<template>
  <div class="overview-content">
    <h2 class="panel-title">Mine klasserom</h2>
    <div
      v-if="error"
      class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded text-sm"
    >
      {{ error }}
    </div>

    <div v-if="isLoading" class="text-center text-white py-6">Laster...</div>
    <div v-else-if="classrooms.length === 0" class="text-center text-white py-6 text-sm">
      Du har ingen klasserom enda.
    </div>

    <div class="row-list">
      <article
        v-for="(classroom, i) in classrooms"
        :key="classroom.id"
        class="row"
        :style="{ animationDelay: `${i * 0.05}s` }"
      >
        <span class="row-title">{{ classroom.title }}</span>

        <span class="row-stat">
          <Users :size="18" aria-hidden="true" />
          {{ studentCount(i) }}
        </span>
        <span class="row-stat row-stat-code">{{ classroom.joinCode }}</span>

        <span
          v-if="classroomNotifications(classroom.id!)"
          class="row-badge"
          :aria-label="`${classroomNotifications(classroom.id!)} uleste varsler`"
        >{{ classroomNotifications(classroom.id!) }}</span>

        <div class="row-actions">
          <button
            class="btn-bare action-btn"
            @click="router.push({ name: 'classroom-detail', params: { id: classroom.id } })"
          >
            Åpne
          </button>
          <button
            class="btn-bare action-btn action-btn-danger"
            @click="classroom.id && deleteClassroom(classroom.id)"
          >
            <Trash2 :size="18" aria-hidden="true" />
            Slett
          </button>
        </div>
      </article>

      <button
        class="btn-bare create-row"
        :style="{ animationDelay: `${classrooms.length * 0.05}s` }"
        @click="router.push({ name: 'create-classroom' })"
        aria-label="Opprett nytt klasserom"
      >
        <Plus :size="18" aria-hidden="true" />
        <span>Nytt klasserom</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.overview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  margin-top: 1.5rem;
}

.panel-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  text-align: center;
  margin-bottom: 0.5rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-shadow: none;
  -webkit-text-stroke: 0;
  paint-order: normal;
}

.row-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0.875rem;
  backdrop-filter: blur(4px);
  max-height: calc(100vh - 12rem);
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.row-list::-webkit-scrollbar {
  width: 8px;
}
.row-list::-webkit-scrollbar-track {
  background: transparent;
}
.row-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.18);
  border-radius: 4px;
}
.row-list::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 1.5rem;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  clip-path: var(--notch-clip);
  animation: slideUp 0.4s ease-out both;
}

.row-title {
  font-size: 1.125rem;
  font-weight: 800;
  color: #111827;
  flex: 1;
  min-width: 4rem;
  -webkit-text-stroke: 0;
  text-shadow: none;
  word-break: break-word;
}

.row-stat {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #374151;
  white-space: nowrap;
  flex-shrink: 0;
}

.row-stat-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-weight: 700;
  color: #4b5563;
  letter-spacing: 0.06em;
}

.row-badge {
  background: #dc2626;
  color: white;
  font-size: 0.875rem;
  font-weight: 700;
  border-radius: 9999px;
  min-width: 1.75rem;
  height: 1.75rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.6rem;
  flex-shrink: 0;
}

.row-actions {
  display: flex;
  gap: 0.6rem;
  flex-shrink: 0;
  margin-left: auto;
}

.action-btn {
  position: relative;
  isolation: isolate;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 2.75rem;
  padding: 0.55rem 1.15rem;
  border: none;
  border-radius: 0 !important;
  clip-path: var(--notch-clip);
  background: transparent;
  font-size: 0.9375rem;
  font-weight: 700;
  color: #1f2937;
  cursor: pointer;
}

.action-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: #d1d5db;
  z-index: -2;
}

.action-btn::after {
  content: '';
  position: absolute;
  inset: 1px;
  clip-path: var(--notch-clip);
  background: white;
  z-index: -1;
  transition: background 0.15s;
}

.action-btn:hover::after {
  background: #f9fafb;
}

.action-btn:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

.action-btn-danger {
  color: #b91c1c;
}

.action-btn-danger::before {
  background: #fca5a5;
}

.action-btn-danger:hover::after {
  background: #fef2f2;
}

.create-row {
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  min-height: 3rem;
  padding: 0.85rem 1.5rem;
  border: none;
  border-radius: 0 !important;
  clip-path: var(--notch-clip);
  background: transparent;
  color: #ffffff;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  animation: slideUp 0.4s ease-out both;
}

.create-row::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.9);
  z-index: -2;
  transition: background 0.15s;
}

.create-row::after {
  content: '';
  position: absolute;
  inset: 2px;
  clip-path: var(--notch-clip);
  background: rgba(40, 30, 20, 0.55);
  backdrop-filter: blur(2px);
  z-index: -1;
  transition: background 0.15s;
}

.create-row:hover::before {
  background: #ffffff;
}

.create-row:hover::after {
  background: rgba(40, 30, 20, 0.35);
}

.create-row:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
