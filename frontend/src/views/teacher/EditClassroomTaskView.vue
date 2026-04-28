<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { getClassroomOptions } from '@/generated-client/@tanstack/vue-query.gen'
import ClassroomService from '@/services/ClassroomService'
import { updateTeacherTask } from '@/services/TaskService'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const classroomId = route.params.id as string
const taskId = route.params.taskId as string

const { data: classroomData } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroom = computed(() => classroomData.value?.data ?? null)

const tasksQuery = useQuery({
  queryKey: ['teacher-classroom-tasks', classroomId],
  queryFn: () => ClassroomService.getClassroomTasks(classroomId, false, false),
})

const classroomTask = computed(() => tasksQuery.data.value?.find(t => t.taskId === taskId))

const taskTitle = ref<string>(classroomTask.value?.task.title ?? '')
const taskDifficulty = ref<number>(classroomTask.value?.task.difficulty ?? 1)
const sortOrder = ref<number>(classroomTask.value?.sortOrder ?? 0)
const isActive = ref<boolean>(classroomTask.value?.isActive ?? true)
const serverError = ref<string | null>(null)

const updateMutation = useMutation({
  mutationFn: async () => {
    await updateTeacherTask(taskId, {
      title: taskTitle.value,
      difficulty: taskDifficulty.value,
    })
    await ClassroomService.updateClassroomTask(classroomId, taskId, {
      sortOrder: sortOrder.value,
      isActive: isActive.value,
    })
  },
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['teacher-classroom-tasks', classroomId] })
    router.push({ name: 'classroom-tasks', params: { id: classroomId } })
  },
  onError: (error: { response?: { data?: { message?: string } } }) => {
    serverError.value = error.response?.data?.message || 'Kunne ikke oppdatere oppgave'
  },
})

async function handleSubmit() {
  serverError.value = null
  if (!taskTitle.value.trim()) {
    serverError.value = 'Tittel kan ikke være tom'
    return
  }
  updateMutation.mutate()
}

function handleCancel() {
  router.push({ name: 'classroom-tasks', params: { id: classroomId } })
}
</script>

<template>
  <SceneComponent
    :title="classroom?.title ? `Rediger oppgave: ${classroom.title}` : 'Rediger oppgave'"
    :previous="`/classroom/${classroomId}/tasks`"
    background="/StartBackground.jpg"
  >
    <div class="edit-task-page">
      <div v-if="tasksQuery.isLoading.value" class="edit-state">Laster...</div>
      <div v-else-if="!classroomTask" class="edit-state edit-state--error">
        Oppgave ikke funnet.
      </div>
      <form v-else @submit.prevent="handleSubmit" class="edit-form">
        <div class="edit-form-group">
          <label class="edit-label" for="taskTitle">Oppgavetittel</label>
          <p v-if="classroomTask.task.title !== taskTitle" class="edit-help edit-help--old">
            Gammelt navn: {{ classroomTask.task.title }}
          </p>
          <input
            id="taskTitle"
            v-model="taskTitle"
            type="text"
            class="edit-input"
            required
            :placeholder="classroomTask.task.title || 'Skriv inn oppgavetittel'"
          />
        </div>

        <div class="edit-form-group">
          <label class="edit-label" for="taskDifficulty">Vanskelighetsgrad</label>
          <input
            id="taskDifficulty"
            v-model.number="taskDifficulty"
            type="number"
            min="1"
            max="5"
            class="edit-input"
            required
          />
          <p class="edit-help">1 = Lett, 5 = Vanskelig</p>
        </div>

        <div class="edit-form-group">
          <label class="edit-label" for="sortOrder">Sortering</label>
          <input
            id="sortOrder"
            v-model.number="sortOrder"
            type="number"
            min="0"
            max="999"
            class="edit-input"
            required
          />
          <p class="edit-help">Lavere tall vises først i listen</p>
        </div>

        <div class="edit-form-group">
          <label class="edit-label" for="isActive">Status</label>
          <select id="isActive" v-model="isActive" class="edit-select">
            <option :value="true">Aktiv</option>
            <option :value="false">Inaktiv</option>
          </select>
          <p class="edit-help">Aktive oppgaver er synlige for studenter</p>
        </div>

        <div v-if="serverError" class="edit-error">
          {{ serverError }}
        </div>

        <div class="edit-actions">
          <button
            type="button"
            class="edit-btn edit-btn--secondary"
            @click="handleCancel"
            :disabled="updateMutation.isPending.value"
          >
            Avbryt
          </button>
          <button
            type="submit"
            class="edit-btn edit-btn--primary"
            :disabled="updateMutation.isPending.value"
          >
            Lagre endringer
          </button>
        </div>
      </form>
    </div>
  </SceneComponent>
</template>

<style scoped>
.edit-task-page {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.edit-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 180px;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.14);
  color: white;
  font-weight: 700;
}

.edit-state--error {
  color: #fecaca;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.edit-form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.edit-label {
  color: white;
  font-weight: 700;
  font-size: 0.9rem;
}

.edit-input,
.edit-select {
  background: white;
  border: 2px solid rgba(120, 53, 15, 0.28);
  border-radius: 0.5rem;
  padding: 0.75rem;
  font-size: 1rem;
  font-weight: 600;
  color: #2b1606;
}

.edit-input:focus,
.edit-select:focus {
  outline: none;
  border-color: #92400e;
}

.edit-help {
  color: #ffedd5;
  font-size: 0.8rem;
  margin: 0;
}

.edit-help--old {
  color: #a1a1aa;
  font-style: italic;
  margin-bottom: 0.25rem;
}

.edit-error {
  background: #fee2e2;
  color: #991b1b;
  padding: 0.75rem;
  border-radius: 0.5rem;
  font-weight: 700;
  text-align: center;
}

.edit-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.edit-btn {
  flex: 1;
  padding: 0.875rem 1.5rem;
  border: none;
  border-radius: 0.75rem;
  font-size: 1rem;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.2s ease;
}

.edit-btn--primary {
  background: #92400e;
  color: white;
  border-color: #78350f;
  -webkit-text-stroke: 1.5px #000;
  paint-order: stroke fill;
  text-shadow:
    2px 0 #000,
    -2px 0 #000,
    0 2px #000,
    0 -2px #000,
    1px 1px #000,
    -1px -1px #000,
    1px -1px #000,
    -1px 1px #000;
}

.edit-btn--primary:hover:not(:disabled) {
  background: #78350f;
}

.edit-btn--secondary {
  background: rgba(255, 255, 255, 0.9);
  color: #2b1606;
  border: 2px solid rgba(120, 53, 15, 0.28);
}

.edit-btn--secondary:hover:not(:disabled) {
  background: rgba(255, 249, 230, 0.95);
}

.edit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .edit-actions {
    flex-direction: column;
  }
}
</style>