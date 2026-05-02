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
      <div class="message-box" aria-live="polite">
        <p class="bra-jobbet">Bra jobbet!</p>
        <p class="klarte-oppgaven">Du klarte oppgaven</p>
      </div>

      <div class="actions">
        <button class="next-btn" @click="goToNextTask" aria-label="Gå til neste oppgave">
          Neste oppgave &rarr;
        </button>
        <button class="exit-btn" @click="exit" aria-label="Avslutt og gå til kartet">
          Avslutt
        </button>
      </div>
    </div>
  </SceneComponent>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import { useStopStore } from '@/stores/StopStore'
import { useAudioStore } from '@/stores/AudioStore'
import { STOP_OUTROS } from '@/data/taskDialogs/outros'

const route = useRoute()
const router = useRouter()
const stopStore = useStopStore()
const audio = useAudioStore()

const stopId = route.query.stop as string | undefined
const taskUuid = route.query.taskId as string | undefined

if (stopId && taskUuid) {
  stopStore.markComplete(stopId, taskUuid)
}

const outroScene = computed(() => (stopId ? STOP_OUTROS[stopId]?.passed ?? null : null))
const showOutro = ref(Boolean(outroScene.value))

// "All complete" is only meaningful once we have the task list. We compute
// it lazily on click instead of in setup so a stale store doesn't lie.
function isStopCleared(): boolean {
  return Boolean(stopId && stopStore.isAllComplete(stopId))
}

function onOutroDone() {
  // The detective's "saken er løst" wraps the moment. If the player just
  // cleared the entire stop, jump straight to the trophy/ending screen
  // instead of stopping on an interim "Neste oppgave" page.
  if (!stopId) {
    showOutro.value = false
    return
  }
  if (isStopCleared()) {
    if (stopId === 'datasenteret') {
      router.push({ name: 'ending' })
    } else {
      router.push({ name: 'stop-finished', query: { stop: stopId } })
    }
    return
  }
  showOutro.value = false
}

onMounted(() => {
  audio.playOneShot('/music/FunFunFunFun.mp3')
  // Make sure the next-task lookup has fresh data when the user clicks
  // through. In test mode we already have everything; otherwise reload.
  if (stopId && !stopStore.testModeClassroomId) {
    stopStore.loadTasks(stopId, true).catch(() => {})
  }
})

function goToNextTask() {
  const testCid = stopStore.testModeClassroomId
  if (stopId && stopStore.isAllComplete(stopId)) {
    if (stopId === 'datasenteret') {
      router.push({ name: 'ending' })
    } else {
      router.push({ name: 'stop-finished', query: { stop: stopId } })
    }
    return
  }
  if (stopId) {
    const tasks = stopStore.tasks[stopId] ?? []
    const next = tasks.find((t) => t.isUnlocked && !stopStore.isComplete(stopId, t.uuid))
    const query = testCid ? `?test=true&classroomId=${testCid}` : ''
    if (next) {
      router.push(`/stops/${stopId}/${next.uuid}${query}`)
    } else {
      router.push(`/stops/${stopId}${query}`)
    }
    return
  }
  if (testCid) {
    router.push(`/classroom/${testCid}/test-game`)
  } else {
    router.push({ name: 'game-map' })
  }
}

function exit() {
  const testCid = stopStore.testModeClassroomId
  if (testCid) {
    router.push(`/classroom/${testCid}/test-game`)
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

.message-box {
  text-align: center;
  animation: popIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}

.bra-jobbet {
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 2px 20px rgba(255, 215, 0, 0.5);
  margin: 0;
}

.klarte-oppgaven {
  font-size: clamp(1.25rem, 3vw, 2rem);
  font-weight: 600;
  color: #f0e6ff;
  margin: 0.25rem 0 0;
}

.actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
}

.next-btn,
.exit-btn {
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

.next-btn {
  background: #ffffff;
  color: #1a0a2e;
}

.exit-btn {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  border: 2px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}

.next-btn:hover,
.exit-btn:hover {
  transform: scale(1.05) translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.next-btn:active,
.exit-btn:active {
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
