<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import FotografenComponent from '../components/FotografenComponent.vue'
import { fetchTaskById } from '@/services/TaskService'
import TaskAttemptService from '@/services/TaskAttemptService'
import { fetchClassroomId } from '@/services/StopService'
import { useStopStore } from '@/stores/StopStore'
import type { TaskDTO, FotografenChallenge, PhotoChallengeContent, NewsChallengeContent, PhotoAnswers } from '@/types'
import { TaskType } from '@/types'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId as string
const stopStore = useStopStore()
const testClassroomId = stopStore.testModeClassroomId
const previousRoute = testClassroomId
  ? `/stops/fotografen?test=true&classroomId=${testClassroomId}`
  : '/stops/fotografen'

const task = ref<TaskDTO | null>(null)
const isLoading = ref(true)
const error = ref('')
const submitting = ref(false)

onMounted(async () => {
  console.log('[FotografenTaskView] Loading task with ID:', taskId)
  console.log('[FotografenTaskView] Test mode classroom ID:', testClassroomId)
  
  try {
    if (testClassroomId) {
      const stopTasks = stopStore.tasks['fotografen'] || []
      const taskFromStore = stopTasks.find(t => t.uuid === taskId)
      
      if (taskFromStore) {
        console.log('[FotografenTaskView] Found task in store:', taskFromStore)
        task.value = {
          id: taskFromStore.uuid,
          title: taskFromStore.title,
          taskType: taskFromStore.taskType as TaskType,
          difficulty: 1,
          isActive: true,
          stopLocation: 'fotografen',
          content: taskFromStore.content,
        }
        console.log('[FotografenTaskView] Task from store content:', task.value?.content)
      } else {
        console.warn('[FotografenTaskView] Task not found in store, fetching from API')
        task.value = await fetchTaskById(taskId)
      }
    } else {
      task.value = await fetchTaskById(taskId)
    }
    
    console.log('[FotografenTaskView] Final task content:', task.value?.content)

  } catch (err) {
    error.value = 'Kunne ikke laste oppgaven. Prøv igjen.'
    console.error('[FotografenTaskView] Error loading task:', err)
  } finally {
    isLoading.value = false
  }
})

const challenge = computed<FotografenChallenge | null>(() => {
  const rawContent = task.value?.content
  const content = (rawContent as { photo?: PhotoChallengeContent })?.photo ?? rawContent as PhotoChallengeContent | null
  if (!content?.alternatives?.length) return null
  return { alternatives: content.alternatives }
})

const taskQuestion = computed(() => {
  const rawContent = task.value?.content
  return (rawContent as { photo?: PhotoChallengeContent })?.photo?.description ?? (rawContent as PhotoChallengeContent)?.description
})
const taskTip = computed(() => {
  const rawContent = task.value?.content
  return (rawContent as { photo?: PhotoChallengeContent })?.photo?.hint ?? (rawContent as PhotoChallengeContent)?.hint
})

function isCorrectLocally(answers: PhotoAnswers): boolean {
  const rawContent = task.value?.content
  const alts = (rawContent as { photo?: PhotoChallengeContent })?.photo?.alternatives ?? (rawContent as PhotoChallengeContent)?.alternatives ?? []
  const sel = answers.selections
  if (sel.length !== alts.length) return false
  return alts.every((alt: any, i: number) => sel[i]?.type === alt.correctType)
}

function failureReason(answers: PhotoAnswers): string {
  const rawContent = task.value?.content
  const alts = (rawContent as { photo?: PhotoChallengeContent })?.photo?.alternatives ?? (rawContent as PhotoChallengeContent)?.alternatives ?? []
  const sel = answers.selections
  const wrongIndexes = alts
    .map((alt: any, i: number) => (sel[i]?.type !== alt.correctType ? i + 1 : null))
    .filter((i): i is number => i !== null)
  if (wrongIndexes.length === 0) return ''
  return `Feil identifiserte bilde ${wrongIndexes.join(', ')}. Sjekk lys, skygger, og detaljer nøye.`
}

async function onSubmit(answers: PhotoAnswers) {
  if (testClassroomId) {
    routeResult(isCorrectLocally(answers), answers)
    return
  }

  try {
    submitting.value = true
    const classroomId = await fetchClassroomId()
    if (!classroomId) {
      error.value = 'Fant ikke et aktivt klasserom for å lagre oppgaveforsøket.'
      return
    }
    const result = await TaskAttemptService.submitAttempt(taskId, classroomId, answers)
    routeResult(result.passed, answers)
  } catch (err) {
    console.error('Failed to submit attempt:', err)
    error.value = 'Kunne ikke lagre resultatet ditt. Prøv igjen.'
  } finally {
    submitting.value = false
  }
}

function routeResult(passed: boolean, answers: PhotoAnswers) {
  if (passed) {
    router.push({
      name: 'task-finished',
      query: { stop: 'fotografen', taskId, ...(testClassroomId ? { classroomId: testClassroomId } : {}) },
    })
  } else {
    router.push({
      name: 'task-failed',
      query: {
        stop: 'fotografen',
        taskId,
        reason: failureReason(answers),
        ...(testClassroomId ? { classroomId: testClassroomId } : {}),
      },
    })
  }
}
</script>

<template>
  <SceneComponent
    :title="task?.title ?? 'Fotografen'"
    :previous="previousRoute"
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
        @submit="onSubmit"
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
