<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import NyhetskvartaletComponent from '../components/NyhetskvartaletComponent.vue'
import { fetchTaskById } from '@/services/TaskService'
import TaskAttemptService from '@/services/TaskAttemptService'
import { fetchClassroomId } from '@/services/StopService'
import { useStopStore } from '@/stores/StopStore'
import type { TaskDTO, NewsChallenge } from '@/types'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId as string
const stopStore = useStopStore()
const testClassroomId = stopStore.testModeClassroomId
const previousRoute = testClassroomId
  ? `/stops/nyhetskvartalet?test=true&classroomId=${testClassroomId}`
  : '/stops/nyhetskvartalet'

const task = ref<TaskDTO | null>(null)
const isLoading = ref(true)
const error = ref('')
const submitting = ref(false)

onMounted(async () => {
  console.log('[NyhetskvartaletTaskView] Loading task with ID:', taskId)
  console.log('[NyhetskvartaletTaskView] Test mode classroom ID:', testClassroomId)
  
  try {
    if (testClassroomId) {
      const stopTasks = stopStore.tasks['nyhetskvartalet'] || []
      const taskFromStore = stopTasks.find(t => t.uuid === taskId)
      
      if (taskFromStore) {
        console.log('[NyhetskvartaletTaskView] Found task in store:', taskFromStore)
        task.value = {
          id: taskFromStore.uuid,
          title: taskFromStore.title,
          taskType: taskFromStore.taskType as TaskType,
          difficulty: 1,
          isActive: true,
          stopLocation: 'nyhetskvartalet',
          content: taskFromStore.content
        }
        console.log('[NyhetskvartaletTaskView] Task from store content:', task.value?.content)
        console.log('[NyhetskvartaletTaskView] Task from store content.news:', task.value?.content?.news)
      } else {
        console.warn('[NyhetskvartaletTaskView] Task not found in store, fetching from API')
        task.value = await fetchTaskById(taskId)
      }
    } else {
      task.value = await fetchTaskById(taskId)
    }
    
    console.log('[NyhetskvartaletTaskView] Final task object:', task.value)
    console.log('[NyhetskvartaletTaskView] Final task content:', task.value?.content)

  } catch (err) {
    error.value = 'Kunne ikke laste oppgaven. Prøv igjen.'
    console.error('[NyhetskvartaletTaskView] Error loading task:', err)
  } finally {
    isLoading.value = false
  }
})

const challenge = computed<NewsChallenge | null>(() => {
  const content = task.value?.content?.news
  if (!content?.articles?.length) return null
  return { articles: content.articles }
})

const taskQuestion = computed(() => task.value?.content?.news?.question)
const taskTip = computed(() => task.value?.content?.news?.hint)

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
  if (!testClassroomId) {
    const wasSaved = await submitAttempt(100)
    if (!wasSaved) return
  }
  router.push({
    name: 'task-finished',
    query: { stop: 'nyhetskvartalet', taskId, ...(testClassroomId ? { classroomId: testClassroomId } : {}) },
  })
}

async function onFailed(reason: string) {
  if (!testClassroomId) {
    const wasSaved = await submitAttempt(0)
    if (!wasSaved) return
  }
  router.push({
    name: 'task-failed',
    query: { stop: 'nyhetskvartalet', taskId, reason, ...(testClassroomId ? { classroomId: testClassroomId } : {}) },
  })
}
</script>

<template>
  <SceneComponent
    :title="task?.title ?? 'Nyhetskvartalet'"
    :previous="previousRoute"
    background="/NyhetskvartaletBackground.png"
  >
    <div v-if="isLoading" class="state-msg">Laster oppgave...</div>
    <div v-else-if="error" class="state-msg state-msg--error">{{ error }}</div>
    <div v-else-if="!challenge" class="state-msg state-msg--error">Oppgaven mangler innhold.</div>
    <template v-else>
      <div v-if="submitting" class="submit-overlay">Lagrer resultat...</div>
      <NyhetskvartaletComponent
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
