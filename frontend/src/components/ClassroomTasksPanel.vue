<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import ClassroomService from '@/services/ClassroomService'
import type { ClassroomTaskResponseDTO } from '@/types'

const props = defineProps<{ classroomId: string }>()

const router = useRouter()
const queryClient = useQueryClient()

const tasksQuery = useQuery({
  queryKey: ['teacher-classroom-tasks', props.classroomId],
  queryFn: () => ClassroomService.getClassroomTasks(props.classroomId, false, false),
})

const tasks = computed<ClassroomTaskResponseDTO[]>(() => tasksQuery.data.value ?? [])

const deleteTaskMutation = useMutation({
  mutationFn: (taskId: string) => ClassroomService.removeClassroomTask(props.classroomId, taskId),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['teacher-classroom-tasks', props.classroomId] })
  },
})

function handleDeleteTask(taskId: string, taskTitle: string) {
  if (confirm(`Er du sikker på at du vil fjerne oppgaven "${taskTitle}" fra dette klasserommet?`)) {
    deleteTaskMutation.mutate(taskId)
  }
}

function handleEditTask(taskId: string) {
  router.push({ name: 'classroom-task-edit', params: { id: props.classroomId, taskId } })
}

function openCreateTask() {
  router.push({ name: 'classroom-task-create', params: { id: props.classroomId } })
}
</script>

<template>
  <div class="overview-content">
    <div v-if="tasksQuery.isLoading.value" class="text-center text-white py-6">Laster oppgaver...</div>
    <div v-else-if="tasksQuery.error.value" class="text-center text-white py-6 text-sm">
      Kunne ikke hente oppgavene.
    </div>
    <div v-else-if="!tasks.length" class="text-center text-white py-6 text-sm">
      Ingen oppgaver i dette klasserommet enda.
    </div>

    <div class="row-list">
      <article
        v-for="(item, i) in tasks"
        :key="item.taskId"
        class="row"
        :style="{ animationDelay: `${i * 0.05}s` }"
      >
        <span class="row-title">{{ item.task.title }}</span>

        <span class="row-stat">{{ item.task.taskType }}</span>
        <span class="row-stat">{{ item.task.stopLocation || 'ukjent stopp' }}</span>
        <span class="row-stat">niv. {{ item.task.difficulty }}</span>

        <span
          class="row-badge"
          :class="item.isActive ? 'badge-active' : 'badge-inactive'"
        >
          {{ item.isActive ? 'Aktiv' : 'Inaktiv' }}
        </span>

        <div class="row-actions">
          <button class="btn-bare action-btn" @click="handleEditTask(item.taskId)">
            <Pencil :size="18" aria-hidden="true" />
            Rediger
          </button>
          <button
            class="btn-bare action-btn action-btn-danger"
            :disabled="deleteTaskMutation.isPending.value"
            @click="handleDeleteTask(item.taskId, item.task.title)"
          >
            <Trash2 :size="18" aria-hidden="true" />
            Slett
          </button>
        </div>
      </article>

      <button
        class="btn-bare create-row"
        :style="{ animationDelay: `${tasks.length * 0.05}s` }"
        @click="openCreateTask"
        aria-label="Lag ny oppgave"
      >
        <Plus :size="18" aria-hidden="true" />
        <span>Lag ny oppgave</span>
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
  margin-top: 0.5rem;
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
  height: calc(100vh - 14rem);
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 6rem;
  -webkit-text-stroke: 0;
  text-shadow: none;
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

.row-badge {
  font-size: 0.875rem;
  font-weight: 700;
  border-radius: 9999px;
  padding: 0.3rem 0.75rem;
  white-space: nowrap;
  flex-shrink: 0;
}

.badge-active {
  background: #dcfce7;
  color: #166534;
}

.badge-inactive {
  background: #fef3c7;
  color: #92400e;
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

.action-btn:hover:not(:disabled)::after {
  background: #f9fafb;
}

.action-btn:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn-danger {
  color: #b91c1c;
}

.action-btn-danger::before {
  background: #fca5a5;
}

.action-btn-danger:hover:not(:disabled)::after {
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
