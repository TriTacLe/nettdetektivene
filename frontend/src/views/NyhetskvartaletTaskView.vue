<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import NyhetskvartaletComponent from '../components/NyhetskvartaletComponent.vue'
import { fetchTaskById } from '@/services/TaskService'
import type { TaskDTO, NewsChallenge } from '@/types'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId as string

const task = ref<TaskDTO | null>(null)
const isLoading = ref(true)
const error = ref('')

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

const challenge = computed<NewsChallenge | null>(() => {
  const content = task.value?.content?.news
  if (!content?.articles?.length) return null
  return { articles: content.articles }
})

const taskQuestion = computed(() => task.value?.content?.news?.question)
const taskTip = computed(() => task.value?.content?.news?.hint)

function onComplete() {
  router.push({
    name: 'task-finished',
    query: { stop: 'nyhetskvartalet', taskId },
  })
}

function onFailed(reason: string) {
  router.push({
    name: 'task-failed',
    query: { stop: 'nyhetskvartalet', taskId, reason },
  })
}
</script>

<template>
  <SceneComponent
    :title="task?.title ?? 'Nyhetskvartalet'"
    previous="/stops/nyhetskvartalet"
    background="/NyhetskvartaletBackground.png"
  >
    <div v-if="isLoading" class="state-msg">Laster oppgave...</div>
    <div v-else-if="error" class="state-msg state-msg--error">{{ error }}</div>
    <div v-else-if="!challenge" class="state-msg state-msg--error">Oppgaven mangler innhold.</div>
    <NyhetskvartaletComponent
      v-else
      :challenge="challenge"
      :question="taskQuestion"
      :tip="taskTip"
      @complete="onComplete"
      @failed="onFailed"
    />
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
</style>
