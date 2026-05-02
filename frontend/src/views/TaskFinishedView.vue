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

      <div class="actions btn-stack">
        <button type="button" @click="goToNextTask" aria-label="Gå til neste oppgave">
          Neste oppgave &rarr;
        </button>
        <button type="button" @click="exit" aria-label="Avslutt og gå til kartet">
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

// Capture state BEFORE markComplete so we can tell whether this run actually
// cleared the stop. Replaying a task that was already passed shouldn't
// trigger the trophy screen — only the transition from "some left" to
// "all done" should.
const wasAlreadyPassed = stopId && taskUuid ? stopStore.isComplete(stopId, taskUuid) : false
if (stopId && taskUuid) {
  stopStore.markComplete(stopId, taskUuid)
}
const justClearedStop = Boolean(
  !wasAlreadyPassed && stopId && stopStore.isAllComplete(stopId),
)

const outroScene = computed(() => (stopId ? STOP_OUTROS[stopId]?.passed ?? null : null))
const showOutro = ref(Boolean(outroScene.value))

function onOutroDone() {
  if (!stopId) {
    showOutro.value = false
    return
  }
  if (justClearedStop) {
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
  // Datasenteret's outro is the hacker — a celebratory jingle clashes with
  // his angry "Hva?! Det skulle ikke være mulig..." beat. The ending
  // cutscene that plays after the final task is where victory music lives.
  if (stopId !== 'datasenteret') {
    audio.playOneShot('/music/FunFunFunFun.mp3')
  }
  // Make sure the next-task lookup has fresh data when the user clicks
  // through. In test mode we already have everything; otherwise reload.
  if (stopId && !stopStore.testModeClassroomId) {
    stopStore.loadTasks(stopId, true).catch(() => {})
  }
})

function goToNextTask() {
  const testCid = stopStore.testModeClassroomId
  if (justClearedStop) {
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
  width: min(22rem, 90vw);
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
