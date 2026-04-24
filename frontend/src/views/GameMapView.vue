<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import { useStopStore } from '@/stores/StopStore'

const stopStore = useStopStore()

const houses = [
  { key: 'student-home', src: '/StudentHomeObject.png', name: 'Detektivkontoret', to: '/student-home', stopId: null,              showStars: false, previousStop: null },
  { key: 'news',         src: '/NewsObject.png',         name: 'Nyhetskvartalet', to: '/stops/nyhetskvartalet', stopId: 'nyhetskvartalet', showStars: true,  previousStop: null },
  { key: 'photo',        src: '/PhotoObject.png',        name: 'Fotografen',       to: '/stops/fotografen',       stopId: 'fotografen',       showStars: true,  previousStop: 'Nyhetskvartalet' },
  { key: 'final-boss',   src: '/FinalBossObject.png',    name: 'Datasenteret',     to: '/stops/datasenteret',     stopId: null,              showStars: false, previousStop: null },
]

const lockedPopup = ref<string | null>(null)

onMounted(() => {
  stopStore.loadTasks('nyhetskvartalet', true)
  stopStore.loadTasks('fotografen', true)
})

function isAvailable(stopId: string | null): boolean {
  if (!stopId) return true
  if (stopStore.loading[stopId]) return true
  const list = stopStore.tasks[stopId]
  if (!list) return true
  return list.some(t => t.isUnlocked)
}

function isAllDone(stopId: string | null): boolean {
  if (!stopId) return false
  return stopStore.isAllComplete(stopId)
}

function filledStars(stopId: string | null): number {
  if (!stopId) return 0
  const total = stopStore.totalTasks(stopId)
  const completed = stopStore.countComplete(stopId)
  if (total === 0 || completed === 0) return 0
  return Math.min(3, Math.ceil(completed * 3 / total))
}

function openLockedPopup(key: string) {
  lockedPopup.value = key
}

function closeLockedPopup() {
  lockedPopup.value = null
}
</script>

<template>
  <SceneComponent title="Utendørs" previous="/student-home" background="/GameMapBackground.jpg">
    <div class="map-objects">
      <div class="road" aria-hidden="true"></div>

      <template v-for="house in houses" :key="house.key">
        <RouterLink
          v-if="isAvailable(house.stopId)"
          :to="house.to"
          class="map-object"
          :class="[house.key, { 'map-object--done': isAllDone(house.stopId) }]"
          :aria-label="house.name"
        >
          <div class="house-label">
            <div v-if="house.showStars" class="stars">
              <span class="star" :class="{ 'star--done': filledStars(house.stopId) >= 1 }">★</span>
              <span class="star" :class="{ 'star--done': filledStars(house.stopId) >= 2 }">★</span>
              <span class="star" :class="{ 'star--done': filledStars(house.stopId) >= 3 }">★</span>
            </div>
            <div class="name">{{ house.name }}</div>
          </div>
          <img class="house-image" :src="house.src" :alt="house.name" />
        </RouterLink>

        <div
          v-else
          class="map-object map-object--locked"
          :class="house.key"
          :aria-label="`${house.name} - låst`"
          role="button"
          tabindex="0"
          @click="openLockedPopup(house.key)"
          @keydown.enter.space.prevent="openLockedPopup(house.key)"
        >
          <div class="house-label">
            <div class="lock-icon" aria-hidden="true">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
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
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
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
    </div>

    <Transition name="overlay-fade">
      <div
        v-if="lockedPopup !== null"
        class="popup-overlay"
        aria-hidden="true"
        @click="closeLockedPopup"
      ></div>
    </Transition>
  </SceneComponent>
</template>

<style scoped>
.map-objects {
  position: relative;
  flex: 1;
  width: 100%;
}

.road {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 12%;
  background: #1a2a2e;
  border-top: 2px solid #0d1618;
  z-index: 0;
}

.map-object {
  position: absolute;
  width: 16.5%;
  bottom: 5%;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 1;
  text-decoration: none;
  cursor: pointer;
  transform-origin: bottom center;
  transition: transform 0.2s ease, filter 0.2s ease;
}

.map-object:hover:not(.map-object--locked),
.map-object:focus-visible:not(.map-object--locked) {
  transform: scale(1.05);
  filter: brightness(1.1);
}

.map-object:active:not(.map-object--locked) {
  transform: scale(1.02);
}

.map-object:focus-visible {
  outline: 2px solid #ffd447;
  outline-offset: 4px;
  border-radius: 4px;
}

.map-object--locked {
  cursor: pointer;
  filter: grayscale(0.6) brightness(0.7);
}

.map-object--locked:hover,
.map-object--locked:focus-visible {
  filter: grayscale(0.4) brightness(0.8);
}

.locked-popup {
  position: absolute;
  bottom: calc(100% + 0.75rem);
  left: 50%;
  transform: translateX(-50%);
  width: 180px;
  background: #1a2a2e;
  border: 1px solid rgba(255, 212, 71, 0.5);
  border-radius: 8px;
  padding: 0.75rem;
  z-index: 10;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.6);
  color: #ffffff;
  text-align: center;
}

.locked-popup::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: rgba(255, 212, 71, 0.5);
}

.locked-popup__close {
  position: absolute;
  top: 0.35rem;
  right: 0.35rem;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 0.15rem;
  line-height: 1;
  display: flex;
}

.locked-popup__close:hover {
  color: #ffffff;
}

.locked-popup__title {
  font-size: 0.8rem;
  font-weight: 700;
  color: #ffd447;
  margin: 0 0 0.35rem;
}

.locked-popup__body {
  font-size: 0.75rem;
  line-height: 1.4;
  margin: 0;
  color: rgba(255, 255, 255, 0.85);
}

.popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 5;
}

.popup-enter-active,
.popup-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
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

.student-home {
  left: 8%;
}

.news {
  left: 28%;
}

.photo {
  left: 50%;
}

.final-boss {
  left: 72%;
}
</style>
