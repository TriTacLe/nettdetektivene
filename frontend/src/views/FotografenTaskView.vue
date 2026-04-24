<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import FotografenComponent from '../components/FotografenComponent.vue'
import { fetchTaskById } from '@/services/TaskService'
import TaskAttemptService from '@/services/TaskAttemptService'
import { fetchClassroomId } from '@/services/StopService'
import type { TaskDTO, FotografenChallenge } from '@/types'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId as string

const task = ref<TaskDTO | null>(null)
const isLoading = ref(true)
const error = ref('')
const submitting = ref(false)

onMounted(async () => {
  try {
    task.value = await fetchTaskById(taskId)
  } catch (err) {
    error.value = 'Kunne ikke laste oppgaven. Prøv igjen.'
    console.error(err)
  } finally {
    isLoading.value = false
  }
})

const challenge = computed<FotografenChallenge | null>(() => {
  const content = task.value?.content?.photo
  if (!content?.alternatives?.length) return null
  return { alternatives: content.alternatives }
})

const taskQuestion = computed(() => task.value?.content?.photo?.description)
const taskTip = computed(() => task.value?.content?.photo?.hint)

async function submitAttempt(score: number) {
  try {
    submitting.value = true
    const classroomId = await fetchClassroomId()
    if (classroomId) {
      await TaskAttemptService.submitAttempt(taskId, classroomId, score)
      return true
    }
    error.value = 'Fant ikke et aktivt klasserom for å lagre oppgaveforsøket.'
    return false
  } catch (err) {
    console.error('Failed to submit attempt:', err)
    error.value = 'Kunne ikke lagre resultatet ditt. Prøv igjen.'
    return false
  } finally {
    submitting.value = false
  }
}

async function onComplete() {
  const wasSaved = await submitAttempt(100)
  if (!wasSaved) return
  router.push({
    name: 'task-finished',
    query: { stop: 'fotografen', taskId },
  })
}

async function onFailed(reason: string) {
  const wasSaved = await submitAttempt(0)
  if (!wasSaved) return
  router.push({
    name: 'task-failed',
    query: { stop: 'fotografen', taskId, reason },
  })
}
</script>

<template>
  <SceneComponent
    :title="task?.title ?? 'Fotografen'"
    previous="/stops/fotografen"
    background="/FotografenBackground.png"
  >
    <div v-if="isLoading" class="state-msg">Laster oppgave...</div>
    <div v-else-if="error" class="state-msg state-msg--error">{{ error }}</div>
    <div v-else-if="!challenge" class="state-msg state-msg--error">Oppgaven mangler innhold.</div>
    <template v-else>
      <div v-if="submitting" class="submit-overlay">Lagrer resultat...</div>
      <FotografenComponent
        :challenge="challenge"
        :question="taskQuestion"
        :tip="taskTip"
        @complete="onComplete"
        @failed="onFailed"
      />
    </template>
  </SceneComponent>
</template>

<style scoped>
.state-msg {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.1rem;
  font-weight: 600;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

.state-msg--error { color: #fca5a5; }

.submit-overlay {
  position: absolute;
  top: 5.5rem;
  right: 1rem;
  z-index: 5;
  background: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 0.65rem 0.9rem;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 700;
}
</style>
