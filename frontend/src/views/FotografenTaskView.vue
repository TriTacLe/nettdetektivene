<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import FotografenComponent from '../components/FotografenComponent.vue'
import { fetchTaskById } from '@/services/TaskService'
import type { TaskDTO, FotografenChallenge } from '@/types'

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

const challenge = computed<FotografenChallenge | null>(() => {
  const content = task.value?.content?.photo
  if (!content?.alternatives?.length) return null
  return { alternatives: content.alternatives }
})

const taskQuestion = computed(() => task.value?.content?.photo?.description)

function onComplete() {
  router.push({
    name: 'task-finished',
    query: { stop: 'fotografen', taskId },
  })
}

function onFailed(reason: string) {
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
    <FotografenComponent
      v-else
      :challenge="challenge"
      :question="taskQuestion"
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
