<template>
  <CutsceneDialog
    v-if="showOutro && outroScene"
    :scene="outroScene"
    @done="onOutroDone"
  />
  <SceneComponent
    v-else
    title=""
    :previous="stopId ? `/stops/${stopId}` : '/game-map'"
    previous-label="Tilbake"
  >
    <div class="content">
      <p class="heading">Prøv igjen! &#10060;</p>

      <div class="explanation-card" role="alert" aria-live="assertive">
        <p class="card-title">Det var ikke riktig denne gangen.</p>
        <p class="card-hint">Her er hva du bør tenke på:</p>
        <p class="card-reason">{{ reason }}</p>
        <p class="card-encourage">Du kan prøve så mange ganger du vil! Ikke gi opp - du klarer det.</p>
      </div>

      <div class="actions">
        <button class="retry-btn" @click="retry" aria-label="Prøv oppgaven på nytt">
          Prøv på nytt &#x1F504;
        </button>
        <button class="resume-btn" @click="resume" aria-label="Tilbake til stoppestedet">
          Tilbake
        </button>
      </div>
    </div>
  </SceneComponent>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import { STOP_OUTROS } from '@/data/taskDialogs/outros'

const route = useRoute()
const router = useRouter()

const stopId = route.query.stop as string | undefined
const taskId = route.query.taskId as string | undefined
const reason = (route.query.reason as string | undefined) ?? 'Sjekk svarene dine og prøv igjen.'

const outroScene = computed(() => (stopId ? STOP_OUTROS[stopId]?.failed ?? null : null))
const showOutro = ref(Boolean(outroScene.value))

function onOutroDone() {
  showOutro.value = false
}

function retry() {
  if (stopId && taskId) {
    router.push(`/stops/${stopId}/${taskId}`)
  } else if (stopId) {
    router.push(`/stops/${stopId}`)
  } else {
    router.push({ name: 'game-map' })
  }
}

function resume() {
  if (stopId) {
    router.push(`/stops/${stopId}`)
  } else {
    router.push({ name: 'game-map' })
  }
}
</script>

<style scoped>
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  padding: 2rem;
}

.heading {
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.5);
  margin: 0;
  text-align: center;
  animation: popIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}

.explanation-card {
  background: #ffffff;
  border-radius: 1rem;
  padding: 1.75rem 2rem;
  max-width: 680px;
  width: 100%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  animation: popIn 0.5s 0.1s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}

.card-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #dc2626;
  margin: 0;
}

.card-hint {
  font-size: 1rem;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.card-reason {
  font-size: 1rem;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.5;
}

.card-encourage {
  font-size: 1rem;
  color: #1a1a1a;
  margin: 0;
}

.actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
}

.retry-btn,
.resume-btn {
  border: none;
  border-radius: 2rem;
  padding: 0.85rem 2.5rem;
  font-size: 1.2rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
  transition: transform 0.15s, box-shadow 0.15s;
  animation: popIn 0.5s 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
  font-family: inherit;
}

.retry-btn {
  background: #ffffff;
  color: #1a1a1a;
}

.resume-btn {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  border: 2px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}

.retry-btn:hover,
.resume-btn:hover {
  transform: scale(1.05) translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.retry-btn:active,
.resume-btn:active {
  transform: scale(0.98);
}

@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.7) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
