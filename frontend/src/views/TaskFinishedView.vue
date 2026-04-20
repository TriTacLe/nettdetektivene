<template>
  <SceneComponent title="" previous="/game-map" previous-label="Kart">
    <canvas ref="confettiCanvas" class="confetti-canvas" />

    <div class="content">
      <div class="message-box" aria-live="polite">
        <p class="bra-jobbet">Bra jobbet!</p>
        <p class="klarte-oppgaven">Du klarte oppgaven</p>
      </div>

      <button class="next-btn" @click="goToNextTask" aria-label="Gå til neste oppgave">
        Neste oppgave &rarr;
      </button>
    </div>
  </SceneComponent>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'

const router = useRouter()
const confettiCanvas = ref<HTMLCanvasElement | null>(null)

let animationId: number | null = null

// ---- Confetti logic ----
interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  color: string
  size: number
  rotation: number
  rotationSpeed: number
  shape: 'rect' | 'circle' | 'ribbon'
  ribbonLength: number
  wave: number
  waveSpeed: number
  waveAmplitude: number
}

const COLORS = [
  '#FFD700', '#FF6B6B', '#4ECDC4', '#45B7D1',
  '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8',
  '#F7DC6F', '#BB8FCE', '#82E0AA', '#F1948A'
]

function createParticle(canvas: HTMLCanvasElement): Particle {
  const shape = Math.random() < 0.6 ? 'ribbon' : Math.random() < 0.5 ? 'rect' : 'circle'
  return {
    x: Math.random() * canvas.width,
    y: -20,
    vx: (Math.random() - 0.5) * 3,
    vy: Math.random() * 2 + 1.5,
    color: COLORS[Math.floor(Math.random() * COLORS.length)] ?? '#FFD700',
    size: Math.random() * 8 + 5,
    rotation: Math.random() * Math.PI * 2,
    rotationSpeed: (Math.random() - 0.5) * 0.15,
    shape,
    ribbonLength: Math.random() * 20 + 15,
    wave: Math.random() * Math.PI * 2,
    waveSpeed: Math.random() * 0.05 + 0.02,
    waveAmplitude: Math.random() * 2 + 1
  }
}

function startConfetti(canvas: HTMLCanvasElement) {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles: Particle[] = []
  for (let i = 0; i < 180; i++) {
    const p = createParticle(canvas)
    p.y = Math.random() * canvas.height * 0.5
    particles.push(p)
  }

  let spawnTimer = 0

  function draw() {
    if (!ctx) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    spawnTimer++
    if (spawnTimer % 3 === 0 && particles.length < 250) {
      particles.push(createParticle(canvas))
    }

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

      if (p.shape === 'rect') {
        ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2)
      } else if (p.shape === 'circle') {
        ctx.beginPath()
        ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2)
        ctx.fill()
      } else {
        ctx.strokeStyle = p.color
        ctx.lineWidth = 3
        ctx.lineCap = 'round'
        ctx.globalAlpha = 0.85
        ctx.beginPath()
        ctx.moveTo(0, -p.ribbonLength / 2)
        ctx.bezierCurveTo(
          p.size, -p.ribbonLength / 4,
          -p.size, p.ribbonLength / 4,
          0, p.ribbonLength / 2
        )
        ctx.stroke()
      }

      ctx.restore()

      if (p.y > canvas.height + 30) {
        particles.splice(i, 1)
      }
    }

    animationId = requestAnimationFrame(draw)
  }

  draw()
}

onMounted(() => {
  if (confettiCanvas.value) {
    startConfetti(confettiCanvas.value)
  }
})

onUnmounted(() => {
  if (animationId !== null) cancelAnimationFrame(animationId)
})

// ---- Navigation ----
function goToNextTask() {
  // Navigerer til game-map for nå.
  // Bytt ut med riktig rute når TaskView er koblet på.
  router.push({ name: 'game-map' })
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

.next-btn {
  background: #ffffff;
  color: #1a0a2e;
  border: none;
  border-radius: 2rem;
  padding: 0.85rem 2.5rem;
  font-size: 1.2rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
  transition: transform 0.15s, box-shadow 0.15s;
  animation: popIn 0.5s 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}

.next-btn:hover {
  transform: scale(1.05) translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.next-btn:active {
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
