<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import NavigationHeader from '@/components/NavigationHeader.vue'
import { useStopStore } from '@/stores/StopStore'

const route = useRoute()
const router = useRouter()
const classroomId = route.params.id as string
const stopStore = useStopStore()

// Same layout/objects as the student GameMapView, but every stop is open
// (test mode), no lock state, no stars, and links carry ?test=true so the
// stop list / task views know to skip attempt submission.
const houses = [
  {
    key: 'student-home',
    src: '/StudentHomeObject.png',
    name: 'Tilbake til klasserom',
    to: `/classroom/${classroomId}`,
    desktop: { x: 12.3, y: 77.4 },
    mobile: { x: 20, y: 87 },
  },
  {
    key: 'news',
    src: '/NewsObject.png',
    name: 'Nyhetskvartalet',
    to: `/stops/nyhetskvartalet?test=true&classroomId=${classroomId}`,
    desktop: { x: 34.6, y: 60 },
    mobile: { x: 80, y: 64 },
  },
  {
    key: 'photo',
    src: '/PhotoObject.png',
    name: 'Fotografen',
    to: `/stops/fotografen?test=true&classroomId=${classroomId}`,
    desktop: { x: 61.5, y: 73.7 },
    mobile: { x: 20, y: 40 },
  },
  {
    key: 'final-boss',
    src: '/FinalBossObject.png',
    name: 'Datasenteret',
    to: `/stops/datasenteret?test=true&classroomId=${classroomId}`,
    desktop: { x: 87.7, y: 58.8 },
    mobile: { x: 80, y: 17 },
  },
]

let mql: MediaQueryList | null = null
const isMobile = ref(false)

function handleOrientationChange(e: MediaQueryListEvent) {
  isMobile.value = e.matches
}

const activeHouses = computed(() =>
  houses.map((h) => ({
    ...h,
    x: isMobile.value ? h.mobile.x : h.desktop.x,
    y: isMobile.value ? h.mobile.y : h.desktop.y,
  })),
)

function buildCurvePath(pts: { x: number; y: number }[]): string {
  if (pts.length < 2) return ''
  const first = pts[0]
  if (!first) return ''
  let d = `M ${first.x} ${first.y}`
  for (let i = 1; i < pts.length; i++) {
    const p = pts[i - 1]
    const c = pts[i]
    if (!p || !c) continue
    const midX = (p.x + c.x) / 2
    d += ` C ${midX} ${p.y} ${midX} ${c.y} ${c.x} ${c.y}`
  }
  return d
}

const roadPath = computed(() => {
  const yOffset = isMobile.value ? 6 : 9
  return buildCurvePath(activeHouses.value.map((h) => ({ x: h.x, y: h.y + yOffset })))
})

onMounted(async () => {
  mql = window.matchMedia('(max-aspect-ratio: 1/1)')
  isMobile.value = mql.matches
  mql.addEventListener('change', handleOrientationChange)

  stopStore.setTestMode(classroomId)
  try {
    await Promise.all([
      stopStore.loadTasksTestMode(classroomId, 'nyhetskvartalet'),
      stopStore.loadTasksTestMode(classroomId, 'fotografen'),
      stopStore.loadTasksTestMode(classroomId, 'datasenteret'),
    ])
  } catch (error) {
    console.error('Failed to load test mode tasks:', error)
  }
})

onUnmounted(() => {
  mql?.removeEventListener('change', handleOrientationChange)
})
</script>

<template>
  <div class="game-map">
    <picture class="map-bg-wrapper" aria-hidden="true">
      <source media="(max-aspect-ratio: 1/1)" srcset="/bybakgrunn-mobil.svg" />
      <img class="map-bg" src="/bybakgrunn.svg" alt="" aria-hidden="true" />
    </picture>

    <NavigationHeader
      title="Utendørs"
      :previous="`/classroom/${classroomId}`"
      class="map-header"
      @back="router.push(`/classroom/${classroomId}`)"
    />

    <div class="test-mode-badge" aria-label="Testmodus">TESTMODUS</div>

    <svg class="path-layer" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <pattern id="cobblestone" x="0" y="0" width="2" height="1.5" patternUnits="userSpaceOnUse">
          <rect width="2" height="1.5" fill="#3a3a48" />
          <ellipse cx="0.5" cy="0.5" rx="0.38" ry="0.28" fill="#4d4d5d" />
          <ellipse cx="1.4" cy="0.5" rx="0.4" ry="0.3" fill="#56566a" />
          <ellipse cx="0.3" cy="1.1" rx="0.32" ry="0.24" fill="#48485a" />
          <ellipse cx="1.1" cy="1.2" rx="0.4" ry="0.27" fill="#525264" />
        </pattern>
      </defs>
      <path class="road-shadow" :d="roadPath" />
      <path class="road-cobble" :d="roadPath" />
      <path class="road-edge" :d="roadPath" />
    </svg>

    <RouterLink
      v-for="house in activeHouses"
      :key="house.key"
      :to="house.to"
      class="map-object"
      :style="{ left: house.x + '%', top: house.y + '%' }"
      :aria-label="house.name"
    >
      <div class="house-label">
        <div class="name">{{ house.name }}</div>
      </div>
      <img class="house-image" :src="house.src" :alt="house.name" />
    </RouterLink>
  </div>
</template>

<style scoped>
.game-map {
  position: relative;
  width: 100%;
  min-height: 100vh;
}

.map-bg-wrapper {
  position: fixed;
  inset: 0;
  z-index: 0;
}

.map-bg {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.map-header {
  position: relative;
  z-index: 20;
}

.path-layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.road-shadow {
  fill: none;
  stroke: #0a0a14;
  stroke-width: 4.5;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0.8;
}

.road-cobble {
  fill: none;
  stroke: url(#cobblestone);
  stroke-width: 3.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.road-edge {
  fill: none;
  stroke: #6f6f80;
  stroke-width: 3.2;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0.3;
}

.map-object {
  position: absolute;
  width: 14%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 2;
  text-decoration: none;
  cursor: pointer;
  transform-origin: center center;
  transition:
    transform 0.2s ease,
    filter 0.2s ease;
}

@media (max-aspect-ratio: 1/1) {
  .map-object {
    width: 30%;
  }
}

.map-object:hover,
.map-object:focus-visible {
  transform: translate(-50%, -50%) scale(1.05);
  filter: brightness(1.1);
}

.map-object:active {
  transform: translate(-50%, -50%) scale(1.02);
}

.map-object:focus-visible {
  outline: 2px solid #ffd447;
  outline-offset: 4px;
  border-radius: 4px;
}

.house-image {
  width: 100%;
  height: auto;
}

.house-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  margin-bottom: 0.5rem;
  text-align: center;
}

.name {
  color: #ffffff;
  font-size: 0.95rem;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

.test-mode-badge {
  position: absolute;
  top: 2.1rem;
  right: 12rem;
  z-index: 20;
  background: #ff9800;
  color: white;
  padding: 0.4rem 0.9rem;
  border-radius: 0.5rem;
  font-weight: 800;
  font-size: 1.5rem;
  letter-spacing: 0.05em;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

@media (max-width: 480px) {
  .map-object {
    width: 20%;
  }

  .name {
    font-size: 0.75rem;
  }

  .test-mode-badge {
    font-size: 0.72rem;
    padding: 0.25rem 0.55rem;
    top: 1.5rem;
    right: 3.8rem;
  }
}

@media (max-width: 360px) {
  .name {
    font-size: 0.65rem;
  }
}
</style>
