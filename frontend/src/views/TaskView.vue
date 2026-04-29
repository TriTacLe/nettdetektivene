<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import NyhetskvartaletComponent from '../components/NyhetskvartaletComponent.vue'
import FotografenComponent from '../components/FotografenComponent.vue'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import { fetchTaskById } from '@/services/TaskService'
import TaskAttemptService from '@/services/TaskAttemptService'
import { fetchClassroomId } from '@/services/StopService'
import { useStopStore } from '@/stores/StopStore'
import { useDialogRunner } from '@/composables/UseDialogRunner'
import { datasenteretIntro } from '@/data/taskDialogs/datasenteret'
import { useStoryProgressStore, StoryStep } from '@/stores/StoryProgressStore'
import type { TaskDTO, NewsChallenge, FotografenChallenge } from '@/types'
import { TaskType } from '@/types'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId as string
const stopId = route.params.stopId as string
const stopStore = useStopStore()
const testClassroomId = stopStore.testModeClassroomId
const previousRoute = testClassroomId
  ? `/stops/${stopId}?test=true&classroomId=${testClassroomId}`
  : `/stops/${stopId}`

const task = ref<TaskDTO | null>(null)
const isLoading = ref(true)
const error = ref('')
const submitting = ref(false)

const storyStore = useStoryProgressStore()
const introRunner = useDialogRunner(datasenteretIntro, 'start')
const introDone = ref(!!testClassroomId || storyStore.hasSeen(StoryStep.TASK_INTRO_DATASENTERET))
function onIntroAdvance() {
  introRunner.advance()
  if (introRunner.isFinished.value) {
    introDone.value = true
    if (!testClassroomId) storyStore.advance(StoryStep.TASK_INTRO_DATASENTERET)
  }
}

onMounted(async () => {
  try {
    if (testClassroomId) {
      const stopTasks = stopStore.tasks[stopId] || []
      const taskFromStore = stopTasks.find(t => t.uuid === taskId)
      if (taskFromStore) {
        task.value = {
          id: taskFromStore.uuid,
          title: taskFromStore.title,
          taskType: taskFromStore.taskType as TaskType,
          difficulty: 1,
          isActive: true,
          stopLocation: stopId,
          content: taskFromStore.content,
        }
      } else {
        task.value = await fetchTaskById(taskId)
      }
    } else {
      task.value = await fetchTaskById(taskId)
    }
  } catch (err) {
    error.value = 'Kunne ikke laste oppgaven. Prøv igjen.'
    console.error('[TaskView] Error loading task:', err)
  } finally {
    isLoading.value = false
  }
})

const isNews = computed(() => task.value?.taskType === TaskType.NEWS)
const isPhoto = computed(() => task.value?.taskType === TaskType.PHOTO)

const newsChallenge = computed<NewsChallenge | null>(() => {
  const content = task.value?.content?.news
  if (!content?.articles?.length) return null
  return { articles: content.articles }
})

const photoChallenge = computed<FotografenChallenge | null>(() => {
  const content = task.value?.content?.photo
  if (!content?.alternatives?.length) return null
  return { alternatives: content.alternatives }
})

const newsQuestion = computed(() => task.value?.content?.news?.question)
const newsTip = computed(() => task.value?.content?.news?.hint)
const photoQuestion = computed(() => task.value?.content?.photo?.description)
const photoTip = computed(() => task.value?.content?.photo?.hint)

const hasRenderableContent = computed(() => {
  if (isNews.value) return Boolean(newsChallenge.value)
  if (isPhoto.value) return Boolean(photoChallenge.value)
  return false
})

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
    query: { stop: stopId, taskId, ...(testClassroomId ? { classroomId: testClassroomId } : {}) },
  })
}

async function onFailed(reason: string) {
  if (!testClassroomId) {
    const wasSaved = await submitAttempt(0)
    if (!wasSaved) return
  }
  router.push({
    name: 'task-failed',
    query: { stop: stopId, taskId, reason, ...(testClassroomId ? { classroomId: testClassroomId } : {}) },
  })
}
</script>

<template>
  <SceneComponent
    :title="task?.title ?? 'Datasenteret'"
    :previous="previousRoute"
    background="/datasenteret.jpg"
  >
    <div v-if="isLoading" class="state-msg">Laster oppgave...</div>
    <div v-else-if="error" class="state-msg state-msg--error">{{ error }}</div>
    <div v-else-if="!task" class="state-msg state-msg--error">Oppgaven ble ikke funnet.</div>
    <div v-else-if="!hasRenderableContent" class="state-msg state-msg--error">
      Denne oppgavetypen har ikke noe innhold å vise ennå.
    </div>
    <template v-else>
      <div v-if="submitting" class="submit-overlay">Lagrer resultat...</div>

      <NyhetskvartaletComponent
        v-if="introDone && isNews && newsChallenge"
        :challenge="newsChallenge"
        :question="newsQuestion"
        :tip="newsTip"
        @complete="onComplete"
        @failed="onFailed"
      />
      <FotografenComponent
        v-else-if="introDone && isPhoto && photoChallenge"
        :challenge="photoChallenge"
        :question="photoQuestion"
        :tip="photoTip"
        @complete="onComplete"
        @failed="onFailed"
      />

      <CutsceneDialog
        v-if="!introDone && introRunner.currentScene.value"
        :key="introRunner.currentScene.value.id"
        :scene="introRunner.currentScene.value"
        @done="onIntroAdvance"
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
  text-align: center;
  padding: 2rem;
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
