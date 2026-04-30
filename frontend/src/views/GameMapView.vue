<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useStopStore } from '@/stores/StopStore'
import NavigationHeader from '@/components/NavigationHeader.vue'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import secretHackerScene from '@/data/SecretHackerDialog'
import AchievementService from '@/services/AchievementService'

const router = useRouter()

const stopStore = useStopStore()

const houses = [
  {
    key: 'student-home',
    src: '/StudentHomeObject.png',
    name: 'Detektivkontoret',
    to: '/student-home',
    stopId: null,
    showStars: false,
    previousStop: null,
    desktop: { x: 12.3, y: 77.4 },
    mobile: { x: 20, y: 87 },
    helpText: 'Gå tilbake til hjemskjermen'
  },
  {
    key: 'news',
    src: '/NewsObject.png',
    name: 'Nyhetskvartalet',
    to: '/stops/nyhetskvartalet',
    stopId: 'nyhetskvartalet',
    showStars: true,
    previousStop: null,
    desktop: { x: 34.6, y: 60 },
    mobile: { x: 80, y: 64 },
    helpText: 'Første stoppested. Her finner du oppgaver om falske nyheter'
  },
  {
    key: 'photo',
    src: '/PhotoObject.png',
    name: 'Fotografen',
    to: '/stops/fotografen',
    stopId: 'fotografen',
    showStars: true,
    previousStop: 'Nyhetskvartalet',
    desktop: { x: 61.5, y: 73.7 },
    mobile: { x: 20, y: 40 },
    helpText: 'Andre stoppested. Løs oppgaver om bildemanipulering'
  },
  {
    key: 'final-boss',
    src: '/FinalBossObject.png',
    name: 'Datasenteret',
    to: '/stops/datasenteret',
    stopId: 'datasenteret',
    showStars: true,
    previousStop: 'Fotografen',
    desktop: { x: 87.7, y: 58.8 },
    mobile: { x: 80, y: 17 },
    helpText: 'Tredje og siste stoppested. Her testes alt du har lært i en stor finaleoppgave.'
  },
]

const lockedPopup = ref<string | null>(null)
const showHackerDialog = ref(false)
const hackerCaught = ref(false)
const SECRET_HACKER_CHANCE = 0.2
let mql: MediaQueryList | null = null
const isMobile = ref(false)

function maybeTriggerHackerEncounter(e: MouseEvent) {
  if (hackerCaught.value || showHackerDialog.value) return
  const target = e.target as HTMLElement | null
  if (
    target?.closest('.map-object') ||
    target?.closest('.locked-popup') ||
    target?.closest('.map-header') ||
    target?.closest('.cutscene')
  ) {
    return
  }
  if (Math.random() < SECRET_HACKER_CHANCE) {
    showHackerDialog.value = true
  }
}

async function onHackerDialogDone() {
  showHackerDialog.value = false
  hackerCaught.value = true
  try {
    await AchievementService.awardSecret('caught_hacker')
  } catch {
    // Surface nothing — the achievement is a fun easter egg, not load-bearing.
  }
}

function handleOrientationChange(e: MediaQueryListEvent) {
  isMobile.value = e.matches
}

onMounted(() => {
  mql = window.matchMedia('(max-aspect-ratio: 1/1)')
  isMobile.value = mql.matches
  mql.addEventListener('change', handleOrientationChange)

  stopStore.clearSeenPickers()
  stopStore.loadTasks('nyhetskvartalet', true)
  stopStore.loadTasks('fotografen', true)
  stopStore.loadTasks('datasenteret', true)
})

onUnmounted(() => {
  mql?.removeEventListener('change', handleOrientationChange)
})

function isAvailable(stopId: string | null): boolean {
  if (!stopId) return true
  if (stopStore.loading[stopId]) return true
  const list = stopStore.tasks[stopId]
  if (!list) return true
  return list.some((t) => t.isUnlocked)
}

function isAllDone(stopId: string | null): boolean {
  if (!stopId) return false
  return stopStore.isAllComplete(stopId)
}

function totalStars(stopId: string | null): number {
  if (!stopId) return 0
  return stopStore.totalTasks(stopId)
}

function filledStars(stopId: string | null): number {
  if (!stopId) return 0
  return stopStore.countComplete(stopId)
}

function openLockedPopup(key: string) {
  lockedPopup.value = key
}

function closeLockedPopup() {
  lockedPopup.value = null
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

</script>

<template>
  <div class="game-map" @click="maybeTriggerHackerEncounter">
    <picture class="map-bg-wrapper" aria-hidden="true">
      <source media="(max-aspect-ratio: 1/1)" srcset="/bybakgrunn-mobil.svg" />
      <img class="map-bg" src="/bybakgrunn.svg" alt="" aria-hidden="true" />
    </picture>

    <CutsceneDialog
      v-if="showHackerDialog"
      :scene="secretHackerScene"
      @done="onHackerDialogDone"
    />

    <NavigationHeader
      title="Utendørs"
      previous="student-home"
      class="map-header"
      @back="router.push('/student-home')"
    />

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

    <template v-for="house in activeHouses" :key="house.key">
      <RouterLink
        v-if="isAvailable(house.stopId)"
        :to="house.to"
        class="map-object"
        :class="{ 'map-object--done': isAllDone(house.stopId) }"
        :style="{ left: house.x + '%', top: house.y + '%' }"
        :aria-label="house.name"
      >
        <div class="house-label">
          <div v-if="house.showStars && totalStars(house.stopId) > 0" class="stars">
            <span
              v-for="n in totalStars(house.stopId)"
              :key="n"
              class="star"
              :class="{ 'star--done': filledStars(house.stopId) >= n }"
              >★</span
            >
          </div>
          <div class="name">{{ house.name }}</div>
        </div>
        <img class="house-image" :src="house.src" :alt="house.name" />
      </RouterLink>

      <div
        v-else
        class="map-object map-object--locked"
        :style="{ left: house.x + '%', top: house.y + '%' }"
        :aria-label="`${house.name} - låst`"
        role="button"
        tabindex="0"
        @click="openLockedPopup(house.key)"
        @keydown.enter.space.prevent="openLockedPopup(house.key)"
      >
        <div class="house-label">
          <div class="lock-icon" aria-hidden="true">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
            >
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
          </div>
          <div class="name">{{ house.name }}</div>
        </div>
        <img class="house-image" :src="house.src" :alt="house.name" />

        <Transition name="popup">
          <div
            v-if="lockedPopup === house.key"
            class="locked-popup"
            role="dialog"
            :aria-label="`${house.name} er låst`"
            @click.stop
          >
            <button class="locked-popup__close" aria-label="Lukk" @click.stop="closeLockedPopup">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
              >
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
            <p class="locked-popup__title">Låst</p>
            <p class="locked-popup__body">
              Fullfør alle oppgavene i
              <strong>{{ house.previousStop ?? 'forrige stopp' }}</strong>
              for å låse opp dette stedet.
            </p>
          </div>
        </Transition>
      </div>
    </template>

    <Transition name="overlay-fade">
      <div
        v-if="lockedPopup !== null"
        class="popup-overlay"
        aria-hidden="true"
        @click="closeLockedPopup"
      />
    </Transition>

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
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 10;
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
    pointer-events: auto;
    width: 30%;
  }
}

.map-object:hover:not(.map-object--locked),
.map-object:focus-visible:not(.map-object--locked) {
  transform: translate(-50%, -50%) scale(1.05);
  filter: brightness(1.1);
}

.map-object:active:not(.map-object--locked) {
  transform: translate(-50%, -50%) scale(1.02);
}

.map-object:focus-visible {
  outline: 2px solid #ffd447;
  outline-offset: 4px;
  border-radius: 4px;
}

.map-object--locked {
  cursor: pointer;
}

.map-object--locked .house-image,
.map-object--locked .house-label {
  filter: grayscale(0.6) brightness(0.7);
  transition: filter 0.2s ease;
}

.map-object--locked:hover .house-image,
.map-object--locked:hover .house-label,
.map-object--locked:focus-visible .house-image,
.map-object--locked:focus-visible .house-label {
  filter: grayscale(0.4) brightness(0.8);
}

.map-object--done .house-image {
  filter: drop-shadow(0 0 8px rgba(255, 212, 71, 0.8));
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

.stars {
  display: flex;
  gap: 0.15rem;
}

.star {
  color: rgba(255, 255, 255, 0.35);
  font-size: 1.25rem;
  line-height: 1;
  text-shadow: 0 0 4px rgba(0, 0, 0, 0.6);
}

.star--done {
  color: #ffd447;
}

.lock-icon {
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
}

.name {
  color: #ffffff;
  font-size: 0.95rem;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

.locked-popup {
  position: absolute;
  bottom: calc(100% + 0.75rem);
  left: 50%;
  transform: translateX(-50%);
  width: 11rem;
  background: linear-gradient(180deg, #fffdf0 0%, #fdf6d8 100%);
  border-radius: 2px;
  padding: 0.6rem 0.9rem 0.7rem;
  z-index: 10;
  box-shadow:
    0 6px 12px rgba(0, 0, 0, 0.3),
    0 2px 4px rgba(0, 0, 0, 0.18);
  text-align: left;
}

.locked-popup__close {
  position: absolute;
  top: 0.35rem;
  right: 0.35rem;
  background: none;
  border: none;
  color: rgba(45, 55, 72, 0.5);
  cursor: pointer;
  padding: 0.15rem;
  line-height: 1;
  display: flex;
}

.locked-popup__close:hover {
  color: #2d3748;
}

.locked-popup__title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #1a365d;
  margin: 0 0 0.2rem;
}

.locked-popup__body {
  font-size: 0.75rem;
  line-height: 1.3;
  margin: 0;
  color: #2d3748;
}

.popup-enter-active,
.popup-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.popup-enter-from,
.popup-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(4px);
}

.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.15s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}
</style>
