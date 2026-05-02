<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Pencil } from 'lucide-vue-next'
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

const tasks = computed<ClassroomTaskResponseDTO[]>(() => {
  const all = tasksQuery.data.value ?? []
  return [...all].sort((a, b) => {
    if (a.isActive !== b.isActive) return a.isActive ? -1 : 1
    return a.sortOrder - b.sortOrder
  })
})

const toggleActiveMutation = useMutation({
  mutationFn: ({ taskId, isActive }: { taskId: string; isActive: boolean }) =>
    ClassroomService.updateClassroomTask(props.classroomId, taskId, { isActive }),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['teacher-classroom-tasks', props.classroomId] })
  },
})

function toggleActive(taskId: string, currentlyActive: boolean) {
  toggleActiveMutation.mutate({ taskId, isActive: !currentlyActive })
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

    <button
      class="btn-bare create-row"
      @click="openCreateTask"
      aria-label="Lag ny oppgave"
    >
      <Plus :size="18" aria-hidden="true" />
      <span>Lag ny oppgave</span>
    </button>

    <div class="row-list">
      <article
        v-for="(item, i) in tasks"
        :key="item.taskId"
        class="row"
        :class="{ 'row--inactive': !item.isActive }"
        :style="{ animationDelay: `${i * 0.05}s` }"
      >
        <span class="row-title">{{ item.task.title }}</span>

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
            class="btn-bare action-btn"
            :class="item.isActive ? 'action-btn-deactivate' : 'action-btn-activate'"
            :disabled="toggleActiveMutation.isPending.value"
            :aria-label="item.isActive ? `Deaktiver ${item.task.title}` : `Aktiver ${item.task.title}`"
            @click="toggleActive(item.taskId, item.isActive)"
          >
            {{ item.isActive ? 'Deaktiver' : 'Aktiver' }}
          </button>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.overview-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  width: 100%;
  margin-top: 0.5rem;
  overflow: hidden;
}

.row-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0.875rem;
  backdrop-filter: blur(4px);
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
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
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
  flex: 1 1 100%;
  min-width: 0;
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

.row-badge {
  font-size: 0.8125rem;
  font-weight: 700;
  border-radius: 9999px;
  padding: 0.2rem 0.65rem;
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

.row--inactive {
  opacity: 0.55;
  background: rgba(255, 255, 255, 0.7);
}


.action-btn-deactivate {
  color: #92400e;
}

.action-btn-deactivate::before {
  background: #fde68a;
}

.action-btn-deactivate:hover:not(:disabled)::after {
  background: #fef9c3;
}

.action-btn-activate {
  color: #166534;
}

.action-btn-activate::before {
  background: #86efac;
}

.action-btn-activate:hover:not(:disabled)::after {
  background: #dcfce7;
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
  background: rgba(20, 15, 10, 0.75);
  backdrop-filter: blur(2px);
  z-index: -1;
  transition: background 0.15s;
}

.create-row:hover::before {
  background: #ffffff;
}

.create-row:hover::after {
  background: rgba(20, 15, 10, 0.6);
}

.create-row:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

@media (max-width: 950px) {
  .row {
    position: relative;
    display: grid;
    grid-template-columns: 1fr;
    gap: 0.35rem;
    padding: 0.75rem 1.25rem;
  }

  .row-title {
    white-space: normal;
    min-width: 0;
  }

  .row-stat {
    font-size: 0.8125rem;
    display: inline;
  }

  .row-badge {
    position: absolute;
    top: 0.5rem;
    right: 0.75rem;
    font-size: 0.6875rem;
  }

  .row-actions {
    display: flex;
    justify-content: flex-start;
    gap: 0.5rem;
  }

  .action-btn {
    font-size: 0.8125rem;
    padding: 0.45rem 0.85rem;
    min-height: 2.25rem;
  }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
