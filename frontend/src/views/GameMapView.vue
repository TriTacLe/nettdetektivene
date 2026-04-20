<script setup lang="ts">
import { RouterLink } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'

const houses = [
  { key: 'student-home', src: '/StudentHomeObject.png', name: 'Detektivkontoret', to: '/student-home' },
  { key: 'news', src: '/NewsObject.png', name: 'Nyhetskvartalet', to: '/stops/nyhetskvartalet' },
  { key: 'photo', src: '/PhotoObject.png', name: 'Fotografen', to: '/stops/fotografen' },
  { key: 'final-boss', src: '/FinalBossObject.png', name: 'Datasenteret', to: '/stops/datasenteret' },
]
</script>

<template>
  <SceneComponent title="Utendørs" previous="/student-home" background="/GameMapBackground.jpg">
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
          <div class="stars" aria-label="3 av 3 stjerner">
            <span class="star">★</span>
            <span class="star">★</span>
            <span class="star">★</span>
          </div>
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

.stars {
  display: flex;
  gap: 0.15rem;
}

.star {
  color: #ffd447;
  font-size: 1.25rem;
  line-height: 1;
  text-shadow: 0 0 4px rgba(0, 0, 0, 0.6);
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
