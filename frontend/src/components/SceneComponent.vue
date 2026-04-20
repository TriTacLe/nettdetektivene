<script setup lang="ts">
import NavigationHeader from './NavigationHeader.vue'

withDefaults(
  defineProps<{
    title: string
    previous?: string
    next?: string
    background?: string
  }>(),
  {
    background: '/StartBackground.jpg',
  },
)
</script>

<template>
  <div class="scene">
    <img class="scene-background" :src="background" alt="" aria-hidden="true" />
    <div class="scene-overlay" />
    <div class="scene-content">
      <NavigationHeader :title="title" :previous="previous" :next="next" />
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
  height: 100vh;
  overflow: hidden;
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
  opacity: 0.5;
  z-index: 10;
  pointer-events: none;
}

.scene-content {
  position: relative;
  z-index: 20;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.scene-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
