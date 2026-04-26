<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useStopStore } from '@/stores/StopStore'
import ClassroomService from '@/services/ClassroomService'

const stopStore = useStopStore()

const houses = [
  { key: 'teacher-home', src: '/StudentHomeObject.png', name: 'Tilbake til Lærerportal', to: '/teacher-home', stopId: null, showStars: false, previousStop: null },
  { key: 'news',         src: '/NewsObject.png',         name: 'Nyhetskvartalet', to: '/stops/nyhetskvartalet', stopId: 'nyhetskvartalet', showStars: true,  previousStop: null },
  { key: 'photo',        src: '/PhotoObject.png',        name: 'Fotografen',       to: '/stops/fotografen',       stopId: 'fotografen',       showStars: true,  previousStop: 'Nyhetskvartalet' },
  { key: 'final-boss',   src: '/FinalBossObject.png',    name: 'Datasenteret',     to: '/stops/datasenteret',     stopId: null,              showStars: false, previousStop: null },
]

const lockedPopup = ref<string | null>(null)

async function initializeTestMode() {
  try {
    const classrooms = await ClassroomService.getMyClassrooms()
    if (classrooms && classrooms.length > 0) {
      const classroom = classrooms[0]
      await stopStore.loadTasksTestMode(classroom.id, 'nyhetskvartalet')
      await stopStore.loadTasksTestMode(classroom.id, 'fotografen')
    }
  } catch (error) {
    console.error('Failed to load test mode tasks:', error)
  }
}

initializeTestMode()

function isAvailable(stopId: string | null): boolean {
  return true // All stops are available in test mode
}

function openLockedPopup(key: string) {
  lockedPopup.value = key
}

function closeLockedPopup() {
  lockedPopup.value = null
}
</script>

<template>
  <SceneComponent title="Test Modus" previous="/teacher-home" background="/GameMapBackground.jpg">
    <div class="test-mode-badge">TEST MODUS - Alle oppgaver åpne</div>

    <div class="map-objects">
      <div class="road" aria-hidden="true"></div>

      <template v-for="house in houses" :key="house.key">
        <RouterLink
          v-if="isAvailable(house.stopId)"
          :to="house.to"
          class="map-object"
          :class="[house.key]"
          :aria-label="house.name"
        >
          <div class="house-label">
            <div v-if="house.showStars" class="stars">
              <span class="star star--done">★</span>
              <span class="star star--done">★</span>
              <span class="star star--done">★</span>
            </div>
            <div class="name">{{ house.name }}</div>
          </div>
          <img class="house-image" :src="house.src" :alt="house.name" />
        </RouterLink>

        <div
          v-else
          class="map-object map-object--locked"
          :class="house.key"
          @click="openLockedPopup(house.key)"
        >
          <div class="house-label">
            <div class="name">{{ house.name }}</div>
          </div>
          <img class="house-image" :src="house.src" :alt="house.name" />
        </div>
      </template>
    </div>

    <div v-if="lockedPopup" class="locked-popup-overlay" @click="closeLockedPopup">
      <div class="locked-popup" @click.stop>
        <p>Dette stoppestedet er ikke åpent ennå.</p>
        <button @click="closeLockedPopup">Lukk</button>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.map-objects {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.road {
  position: absolute;
  bottom: 10%;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(90deg, #8B7355, #A9A9A9, #8B7355);
  border-radius: 30px;
  z-index: 1;
}

.map-object {
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s ease;
  text-decoration: none;
}

.map-object:hover {
  transform: scale(1.05);
}

.map-object--locked {
  cursor: not-allowed;
  opacity: 0.6;
}

.map-object--locked:hover {
  transform: none;
}

.house-image {
  max-height: 120px;
  max-width: 120px;
  object-fit: contain;
}

.house-label {
  margin-bottom: 0.5rem;
  text-align: center;
}

.name {
  background: white;
  padding: 0.5rem 1rem;
  border-radius: 1rem;
  font-weight: bold;
  font-size: 0.9rem;
  color: #333;
}

.stars {
  color: #FFD700;
  font-size: 1.5rem;
  margin-bottom: 0.25rem;
}

.star:not(.star--done) {
  color: #ccc;
}

.locked-popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.locked-popup {
  background: white;
  padding: 2rem;
  border-radius: 1rem;
  max-width: 90%;
  text-align: center;
}

.locked-popup button {
  margin-top: 1rem;
  padding: 0.5rem 2rem;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 1rem;
}

.test-mode-badge {
  position: absolute;
  top: 1rem;
  right: 1rem;
  background: #ff9800;
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-weight: bold;
  z-index: 20;
}
</style>