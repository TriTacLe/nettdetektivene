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
    <canvas ref="confettiCanvas" class="confetti-canvas" />

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
import { computed, nextTick, onUnmounted, ref, onMounted, watch } from 'vue'
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
const confettiCanvas = ref<HTMLCanvasElement | null>(null)
let animationId: number | null = null

interface Particle {
  x: number; y: number; vx: number; vy: number
  color: string; size: number; rotation: number; rotationSpeed: number
  shape: 'rect' | 'circle' | 'ribbon'
  ribbonLength: number; wave: number; waveSpeed: number; waveAmplitude: number
}

const CONFETTI_COLORS = [
  '#FFD700', '#FF6B6B', '#4ECDC4', '#45B7D1',
  '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8',
]

function createParticle(canvas: HTMLCanvasElement): Particle {
  const shape = Math.random() < 0.6 ? 'ribbon' : Math.random() < 0.5 ? 'rect' : 'circle'
  return {
    x: Math.random() * canvas.width, y: -20,
    vx: (Math.random() - 0.5) * 3, vy: Math.random() * 2 + 1.5,
    color: CONFETTI_COLORS[Math.floor(Math.random() * CONFETTI_COLORS.length)] ?? '#FFD700',
    size: Math.random() * 8 + 5,
    rotation: Math.random() * Math.PI * 2,
    rotationSpeed: (Math.random() - 0.5) * 0.15,
    shape,
    ribbonLength: Math.random() * 20 + 15,
    wave: Math.random() * Math.PI * 2,
    waveSpeed: Math.random() * 0.05 + 0.02,
    waveAmplitude: Math.random() * 2 + 1,
  }
}

function startConfetti(canvas: HTMLCanvasElement) {
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  const particles: Particle[] = []
  for (let i = 0; i < 160; i++) {
    const p = createParticle(canvas)
    p.y = Math.random() * canvas.height * 0.5
    particles.push(p)
  }
  let spawnTimer = 0
  function draw() {
    if (!ctx) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    spawnTimer++
    if (spawnTimer % 4 === 0 && particles.length < 220) particles.push(createParticle(canvas))
    for (let i = particles.length - 1; i >= 0; i--) {
      const p = particles[i]
      if (!p) continue
      p.wave += p.waveSpeed
      p.x += p.vx + Math.sin(p.wave) * p.waveAmplitude
      p.y += p.vy
      p.rotation += p.rotationSpeed
      ctx.save()
      ctx.translate(p.x, p.y)
      ctx.rotate(p.rotation)
      ctx.fillStyle = p.color
      ctx.globalAlpha = 0.9
      if (p.shape === 'rect') ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2)
      else if (p.shape === 'circle') {
        ctx.beginPath(); ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2); ctx.fill()
      } else {
        ctx.strokeStyle = p.color; ctx.lineWidth = 3; ctx.lineCap = 'round'; ctx.globalAlpha = 0.85
        ctx.beginPath()
        ctx.moveTo(0, -p.ribbonLength / 2)
        ctx.bezierCurveTo(p.size, -p.ribbonLength / 4, -p.size, p.ribbonLength / 4, 0, p.ribbonLength / 2)
        ctx.stroke()
      }
      ctx.restore()
      if (p.y > canvas.height + 30) particles.splice(i, 1)
    }
    animationId = requestAnimationFrame(draw)
  }
  draw()
}

watch(showOutro, (v) => {
  if (!v) {
    nextTick(() => {
      if (confettiCanvas.value && animationId === null) startConfetti(confettiCanvas.value)
    })
  }
})

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
  // If there's no outro to play, the buttons screen is rendered immediately.
  if (!showOutro.value && confettiCanvas.value) {
    startConfetti(confettiCanvas.value)
  }
  // Make sure the next-task lookup has fresh data when the user clicks
  // through. In test mode we already have everything; otherwise reload.
  if (stopId && !stopStore.testModeClassroomId) {
    stopStore.loadTasks(stopId, true).catch(() => {})
  }
})

onUnmounted(() => {
  if (animationId !== null) cancelAnimationFrame(animationId)
  audio.stopOneShot()
})

function goToNextTask() {
  audio.stopOneShot()
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
  audio.stopOneShot()
  const testCid = stopStore.testModeClassroomId
  if (testCid) {
    router.push(`/classroom/${testCid}/test-game`)
  } else {
    router.push({ name: 'game-map' })
  }
}
</script>

<style scoped>
.confetti-canvas {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 30;
}

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
