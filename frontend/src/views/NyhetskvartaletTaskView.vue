<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import NyhetskvartaletComponent from '../components/NyhetskvartaletComponent.vue'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import { useDialogRunner } from '@/composables/UseDialogRunner'
import { nyhetskvartaletIntro } from '@/data/taskDialogs/nyhetskvartalet'
import { useStoryProgressStore, StoryStep } from '@/stores/StoryProgressStore'
import { fetchTaskById } from '@/services/TaskService'
import TaskAttemptService from '@/services/TaskAttemptService'
import { fetchClassroomId } from '@/services/StopService'
import { useStopStore } from '@/stores/StopStore'
import type { TaskDTO, NewsArticle, NewsChallengeContent, NewsAnswers } from '@/types'
import { TaskType } from '@/types'

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

const storyStore = useStoryProgressStore()
const introRunner = useDialogRunner(nyhetskvartaletIntro, 'start')
const introDone = ref(!!testClassroomId || storyStore.hasSeen(StoryStep.TASK_INTRO_NYHETSKVARTALET))
function onIntroAdvance() {
  introRunner.advance()
  if (introRunner.isFinished.value) {
    introDone.value = true
    if (!testClassroomId) storyStore.advance(StoryStep.TASK_INTRO_NYHETSKVARTALET)
  }
}

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
          content: taskFromStore.content,
        }
        console.log('[NyhetskvartaletTaskView] Task from store content:', task.value?.content)
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

const challenge = computed(() => {
  const rawContent = task.value?.content
  const content = (rawContent as { news?: NewsChallengeContent })?.news ?? rawContent as NewsChallengeContent | null
  if (!content?.articles?.length) return null
  return { articles: content.articles as [NewsArticle, NewsArticle] }
})

const taskQuestion = computed(() => {
  const rawContent = task.value?.content
  return (rawContent as { news?: NewsChallengeContent })?.news?.question ?? (rawContent as NewsChallengeContent)?.question
})
const taskTip = computed(() => {
  const rawContent = task.value?.content
  return (rawContent as { news?: NewsChallengeContent })?.news?.hint ?? (rawContent as NewsChallengeContent)?.hint
})

function isCorrectLocally(answers: NewsAnswers): boolean {
  const articles = (task.value?.content as { news?: NewsChallengeContent })?.news?.articles ?? []
  const idx = answers.selectedIndex
  return Boolean(articles[idx]?.isReal)
}

const FAILURE_REASON =
  'Du valgte den falske nyheten. Sjekk alltid kilden, se etter troverdige forfattere og vær kritisk til overdrevne overskrifter.'

async function onSubmit(answers: NewsAnswers) {
  if (testClassroomId) {
    routeResult(isCorrectLocally(answers))
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
    routeResult(result.passed)
  } catch (err) {
    console.error('Failed to submit attempt:', err)
    error.value = 'Kunne ikke lagre resultatet ditt. Prøv igjen.'
  } finally {
    submitting.value = false
  }
}

function routeResult(passed: boolean) {
  if (passed) {
    router.push({
      name: 'task-finished',
      query: { stop: 'nyhetskvartalet', taskId, ...(testClassroomId ? { classroomId: testClassroomId } : {}) },
    })
  } else {
    router.push({
      name: 'task-failed',
      query: {
        stop: 'nyhetskvartalet',
        taskId,
        reason: FAILURE_REASON,
        ...(testClassroomId ? { classroomId: testClassroomId } : {}),
      },
    })
  }
}
</script>

<template>
  <SceneComponent
    :title="task?.title ?? 'Nyhetskvartalet'"
    :previous="previousRoute"
    background="/NyhetskvartaletBackground.jpg"
  >
    <div v-if="isLoading" class="state-msg">Laster oppgave...</div>
    <div v-else-if="error" class="state-msg state-msg--error">{{ error }}</div>
    <div v-else-if="!challenge" class="state-msg state-msg--error">Oppgaven mangler innhold.</div>
    <template v-else>
      <div v-if="submitting" class="submit-overlay">Lagrer resultat...</div>
      <NyhetskvartaletComponent
        v-if="introDone"
        :challenge="challenge"
        :question="taskQuestion"
        :tip="taskTip"
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
