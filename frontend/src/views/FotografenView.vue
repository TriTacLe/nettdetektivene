<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import FotografenComponent from '../components/FotografenComponent.vue'
import { fetchFotografenChallenge } from '@/services/ChallengeService'
import type { FotografenChallenge } from '../types'

const route = useRoute()
const router = useRouter()

const taskId = Number(route.params.taskId)
const challenge = ref<FotografenChallenge | null>(null)
const isLoading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    challenge.value = await fetchFotografenChallenge('fotografen', taskId)
  } catch {
    error.value = 'Kunne ikke laste oppgaven. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
})

function onComplete() {
  router.push({ name: 'task-finished', query: { stop: 'fotografen', taskId: String(taskId) } })
}

function onFailed(reason: string) {
  router.push({
    name: 'task-failed',
    query: { stop: 'fotografen', taskId: String(taskId), reason, returnTo: 'fotografen-task' },
  })
}
</script>

<template>
  <SceneComponent title="Fotografen" previous="/stops/fotografen" background="/FotografenBackground.png">
    <div v-if="isLoading" class="state-msg">Laster oppgave...</div>
    <div v-else-if="error" class="state-msg state-msg--error">{{ error }}</div>
    <FotografenComponent v-else-if="challenge" :challenge="challenge" @complete="onComplete" @failed="onFailed" />
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

.state-msg--error {
  color: #fca5a5;
}
</style>
