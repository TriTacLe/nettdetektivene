<script setup lang="ts">
import NavigationHeader from './NavigationHeader.vue'

withDefaults(
  defineProps<{
    title: string
    previous?: string
    previousLabel?: string
    next?: string
    nextLabel?: string
    backgroundImage?: string
    darken?: number
  }>(),
  {
    backgroundImage: '/background.jpg',
    darken: 0.5,
  },
)
</script>

<template>
  <div class="scene">
    <img class="scene-background" :src="backgroundImage" alt="" aria-hidden="true" />
    <div class="scene-overlay" :style="{ opacity: darken }" />
    <div class="scene-content">
      <NavigationHeader
        :title="title"
        :previous="previous"
        :previous-label="previousLabel"
        :next="next"
        :next-label="nextLabel"
      />
      <div class="scene-body">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.scene {
  position: relative;
  width: 100%;
  min-height: 100vh;
}

.scene-background {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}

.scene-overlay {
  position: fixed;
  inset: 0;
  background: black;
  z-index: 10;
  pointer-events: none;
}

.scene-content {
  position: relative;
  z-index: 20;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.scene-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
