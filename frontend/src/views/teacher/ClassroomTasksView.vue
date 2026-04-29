<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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

async function handleDeleteTask(taskId: string, taskTitle: string) {
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
    <div class="flex flex-col items-center flex-1 w-full px-4 sm:px-6 pb-10 pt-4">
      <div class="flex flex-col gap-3 w-full max-w-3xl mx-auto">
        <button class="btn-vn overview-primary-btn" @click="openCreateTask">
          + Lag ny oppgave
        </button>

        <div v-if="tasksQuery.isLoading.value" class="text-center text-white py-6">Laster oppgaver...</div>
        <div v-else-if="tasksQuery.error.value" class="text-center text-white py-6 text-sm">
          Kunne ikke hente oppgavene.
        </div>
        <div v-else-if="!tasks.length" class="text-center text-white py-6 text-sm">
          Ingen oppgaver i dette klasserommet enda.
        </div>

        <div
          v-for="item in tasks"
          :key="item.taskId"
          class="w-full bg-white rounded-xl shadow px-4 py-4 sm:px-5 sm:py-4 flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4"
        >
          <div class="flex-1 min-w-0">
            <p class="font-bold text-gray-900 text-base leading-tight truncate">
              {{ item.task.title }}
            </p>
            <p class="text-gray-500 text-sm mt-0.5">
              {{ item.task.taskType }} · {{ item.task.stopLocation || 'ukjent stopp' }} · vanskelighet {{ item.task.difficulty }}
            </p>
          </div>

          <div class="flex items-center gap-2 sm:gap-3 flex-wrap sm:flex-nowrap">
            <span
              class="text-xs px-2 py-1 rounded-full font-semibold shrink-0"
              :class="item.isActive ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'"
            >
              {{ item.isActive ? 'Aktiv' : 'Inaktiv' }}
            </span>
            <button
              class="btn-vn overview-action-btn"
              @click="handleEditTask(item.taskId)"
            >
              Rediger
            </button>
            <button
              class="btn-vn btn-vn-danger overview-action-btn"
              :disabled="deleteTaskMutation.isPending.value"
              @click="handleDeleteTask(item.taskId, item.task.title)"
            >
              Slett
            </button>
          </div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>
