<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { getClassroomOptions } from '@/generated-client/@tanstack/vue-query.gen'
import ClassroomService from '@/services/ClassroomService'
import type { ClassroomTaskResponseDTO } from '@/types'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const classroomId = route.params.id as string

const { data: classroomData } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroom = computed(() => classroomData.value?.data ?? null)

const tasksQuery = useQuery({
  queryKey: ['teacher-classroom-tasks', classroomId],
  queryFn: () => ClassroomService.getClassroomTasks(classroomId, false, false),
})

const tasks = computed<ClassroomTaskResponseDTO[]>(() => tasksQuery.data.value ?? [])

const deleteTaskMutation = useMutation({
  mutationFn: (taskId: string) => ClassroomService.removeClassroomTask(classroomId, taskId),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['teacher-classroom-tasks', classroomId] })
  },
})

function handleDeleteTask(taskId: string, taskTitle: string) {
  if (confirm(`Er du sikker på at du vil fjerne oppgaven "${taskTitle}" fra dette klasserommet?`)) {
    deleteTaskMutation.mutate(taskId)
  }
}

function handleEditTask(taskId: string) {
  router.push({ name: 'classroom-task-edit', params: { id: classroomId, taskId } })
}

function openCreateTask() {
  router.push({ name: 'classroom-task-create', params: { id: classroomId } })
}
</script>

<template>
  <SceneComponent
    :title="classroom?.title ? `Oppgaver: ${classroom.title}` : 'Oppgaver'"
    :previous="`/classroom/${classroomId}`"
    background="/StartBackground.jpg"
  >
    <div class="overview-content">
      <div v-if="tasksQuery.isLoading.value" class="text-center text-white py-6">Laster oppgaver...</div>
      <div v-else-if="tasksQuery.error.value" class="text-center text-white py-6 text-sm">
        Kunne ikke hente oppgavene.
      </div>
      <div v-else-if="!tasks.length" class="text-center text-white py-6 text-sm">
        Ingen oppgaver i dette klasserommet enda.
      </div>

      <div class="card-list">
        <article
          v-for="(item, i) in tasks"
          :key="item.taskId"
          class="task-card"
          :style="{ animationDelay: `${i * 0.07}s` }"
        >
          <div class="card-header">
            <p class="card-title">{{ item.task.title }}</p>
            <span
              class="card-badge"
              :class="item.isActive ? 'badge-active' : 'badge-inactive'"
            >
              {{ item.isActive ? 'Aktiv' : 'Inaktiv' }}
            </span>
          </div>

          <div class="card-stats">
            <span class="stat">{{ item.task.taskType }}</span>
            <span class="stat">{{ item.task.stopLocation || 'ukjent stopp' }}</span>
            <span class="stat">vanskelighet {{ item.task.difficulty }}</span>
          </div>

          <div class="card-actions">
            <button class="btn-bare action-btn" @click="handleEditTask(item.taskId)">
              <Pencil :size="14" aria-hidden="true" />
              Rediger
            </button>
            <button
              class="btn-bare action-btn action-btn-danger"
              :disabled="deleteTaskMutation.isPending.value"
              @click="handleDeleteTask(item.taskId, item.task.title)"
            >
              <Trash2 :size="14" aria-hidden="true" />
              Slett
            </button>
          </div>
        </article>

        <button
          class="btn-bare create-card"
          :style="{ animationDelay: `${tasks.length * 0.07}s` }"
          @click="openCreateTask"
          aria-label="Lag ny oppgave"
        >
          <Plus :size="24" aria-hidden="true" />
          <span>Lag ny oppgave</span>
        </button>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.overview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 1.5rem 1.5rem;
  max-width: 32rem;
  width: 100%;
  margin: 0 auto;
}

.card-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.task-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  clip-path: var(--notch-clip);
  padding: 1.25rem 1.5rem;
  animation: slideUp 0.5s ease-out both;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.625rem;
}

.card-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #111827;
  margin: 0;
  line-height: 1.3;
  -webkit-text-stroke: 0;
  text-shadow: none;
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

.card-badge {
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: 9999px;
  padding: 0.25rem 0.625rem;
  white-space: nowrap;
}

.badge-active {
  background: #dcfce7;
  color: #166534;
}

.badge-inactive {
  background: #fef3c7;
  color: #92400e;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.stat {
  font-size: 0.875rem;
  font-weight: 500;
  color: #4b5563;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 0 !important;
  clip-path: var(--notch-clip);
  background: transparent;
  font-size: 0.875rem;
  font-weight: 600;
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

.create-card {
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.25rem;
  border: none;
  border-radius: 0 !important;
  clip-path: var(--notch-clip);
  background: transparent;
  color: #ffffff;
  font-size: 0.9375rem;
  font-weight: 600;
  cursor: pointer;
  animation: slideUp 0.5s ease-out both;
}

.create-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.9);
  z-index: -2;
  transition: background 0.15s;
}

.create-card::after {
  content: '';
  position: absolute;
  inset: 2px;
  clip-path: var(--notch-clip);
  background: rgba(40, 30, 20, 0.55);
  backdrop-filter: blur(2px);
  z-index: -1;
  transition: background 0.15s;
}

.create-card:hover::before {
  background: #ffffff;
}

.create-card:hover::after {
  background: rgba(40, 30, 20, 0.35);
}

.create-card:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
