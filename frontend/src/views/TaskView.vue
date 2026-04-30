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
import type {
  TaskDTO,
  NewsChallenge,
  FotografenChallenge,
  NewsAnswers,
  PhotoAnswers,
  TaskAnswers,
  NewsChallengeContent,
  PhotoChallengeContent,
} from '@/types'
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
  const rawContent = task.value?.content
  const content = (rawContent as { news?: NewsChallengeContent })?.news ?? rawContent as NewsChallengeContent | null
  if (!content?.articles?.length || content.articles.length < 2) return null
  return content.articles.slice(0, 2) as unknown as NewsChallenge
})

const photoChallenge = computed<FotografenChallenge | null>(() => {
  const rawContent = task.value?.content
  const content = (rawContent as { photo?: PhotoChallengeContent })?.photo ?? rawContent as PhotoChallengeContent | null
  if (!content?.alternatives?.length) return null
  return { alternatives: content.alternatives }
})

const newsQuestion = computed(() => {
  const rawContent = task.value?.content
  return (rawContent as { news?: NewsChallengeContent })?.news?.question ?? (rawContent as NewsChallengeContent)?.question
})
const newsTip = computed(() => {
  const rawContent = task.value?.content
  return (rawContent as { news?: NewsChallengeContent })?.news?.hint ?? (rawContent as NewsChallengeContent)?.hint
})
const photoQuestion = computed(() => {
  const rawContent = task.value?.content
  return (rawContent as { photo?: PhotoChallengeContent })?.photo?.description ?? (rawContent as PhotoChallengeContent)?.description
})
const photoTip = computed(() => {
  const rawContent = task.value?.content
  return (rawContent as { photo?: PhotoChallengeContent })?.photo?.hint ?? (rawContent as PhotoChallengeContent)?.hint
})

const hasRenderableContent = computed(() => {
  if (isNews.value) return Boolean(newsChallenge.value)
  if (isPhoto.value) return Boolean(photoChallenge.value)
  return false
})

function isCorrectLocally(answers: TaskAnswers): boolean {
  if (isNews.value) {
    const rawContent = task.value?.content
    const articles = (rawContent as { news?: NewsChallengeContent })?.news?.articles ?? (rawContent as NewsChallengeContent)?.articles ?? []
    const idx = (answers as NewsAnswers).selectedIndex
    return Boolean(articles[idx]?.isReal)
  }
  if (isPhoto.value) {
    const rawContent = task.value?.content
    const alts = (rawContent as { photo?: PhotoChallengeContent })?.photo?.alternatives ?? (rawContent as PhotoChallengeContent)?.alternatives ?? []
    const sel = (answers as PhotoAnswers).selections
    if (sel.length !== alts.length) return false
    return alts.every((alt: any, i: number) => sel[i]?.type === alt.correctType)
  }
  return false
}

function failureReason(answers: TaskAnswers): string {
  if (isNews.value) {
    return 'Du valgte den falske nyheten. Sjekk alltid kilden, se etter troverdige forfattere og vær kritisk til overdrevne overskrifter.'
  }
  if (isPhoto.value) {
    const rawContent = task.value?.content
    const alts = (rawContent as { photo?: PhotoChallengeContent })?.photo?.alternatives ?? (rawContent as PhotoChallengeContent)?.alternatives ?? []
    const sel = (answers as PhotoAnswers).selections
    const wrongIndexes = alts
      .map((alt: any, i: number) => (sel[i]?.type !== alt.correctType ? i + 1 : null))
      .filter((i): i is number => i !== null)
    if (wrongIndexes.length === 0) return ''
    return `Feil identifiserte bilde ${wrongIndexes.join(', ')}. Sjekk lys, skygger, og detaljer nøye.`
  }
  return 'Uventet oppgavetype.'
}

async function onSubmit(answers: TaskAnswers) {
  if (testClassroomId) {
    const passed = isCorrectLocally(answers)
    routeResult(passed, answers)
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

function routeResult(passed: boolean, answers: TaskAnswers) {
  if (passed) {
    router.push({
      name: 'task-finished',
      query: { stop: stopId, taskId, ...(testClassroomId ? { classroomId: testClassroomId } : {}) },
    })
  } else {
    router.push({
      name: 'task-failed',
      query: {
        stop: stopId,
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
        @submit="onSubmit"
      />
      <FotografenComponent
        v-else-if="introDone && isPhoto && photoChallenge"
        :challenge="photoChallenge"
        :question="photoQuestion"
        :tip="photoTip"
        @submit="onSubmit"
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
