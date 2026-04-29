<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useStopStore } from '@/stores/StopStore'

const route = useRoute()
const classroomId = route.params.id as string
const stopStore = useStopStore()

// Same layout/objects as the student GameMapView, but every stop is open
// (test mode), no lock state, no stars, and links carry ?test=true so the
// stop list / task views know to skip attempt submission.
const houses = [
  { key: 'student-home', src: '/StudentHomeObject.png', name: 'Tilbake til klasserom', to: `/classroom/${classroomId}` },
  { key: 'news',         src: '/NewsObject.png',         name: 'Nyhetskvartalet',     to: `/stops/nyhetskvartalet?test=true&classroomId=${classroomId}` },
  { key: 'photo',        src: '/PhotoObject.png',        name: 'Fotografen',           to: `/stops/fotografen?test=true&classroomId=${classroomId}` },
  { key: 'final-boss',   src: '/FinalBossObject.png',    name: 'Datasenteret',         to: `/stops/datasenteret?test=true&classroomId=${classroomId}` },
]

onMounted(async () => {
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
</script>

<template>
  <SceneComponent title="Test Modus" :previous="`/classroom/${classroomId}`" background="/GameMapBackground.jpg">
    <div class="test-mode-badge" aria-label="Test modus">TEST MODUS</div>

    <div class="map-objects">
      <div class="road" aria-hidden="true"></div>

      <RouterLink
        v-for="house in houses"
        :key="house.key"
        :to="house.to"
        class="map-object"
        :class="house.key"
        :aria-label="house.name"
      >
        <div class="house-label">
          <div class="name">{{ house.name }}</div>
        </div>
        <img class="house-image" :src="house.src" :alt="house.name" />
      </RouterLink>
    </div>
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

.map-object:hover,
.map-object:focus-visible {
  transform: scale(1.05);
  filter: brightness(1.1);
}

.map-object:active {
  transform: scale(1.02);
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

/* Same horizontal positions as the student map. */
.student-home { left: 8%; }
.news         { left: 28%; }
.photo        { left: 50%; }
.final-boss   { left: 72%; }

.test-mode-badge {
  position: absolute;
  top: 5rem;
  right: 1rem;
  z-index: 20;
  background: #ff9800;
  color: white;
  padding: 0.4rem 0.9rem;
  border-radius: 0.5rem;
  font-weight: 800;
  font-size: 0.85rem;
  letter-spacing: 0.05em;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}
</style>
