<script setup lang="ts">
import { useAttrs } from 'vue'
import { useRouter } from 'vue-router'
import NavigationHeader from './NavigationHeader.vue'

const props = withDefaults(
  defineProps<{
    title: string
    previous?: string
    next?: string
    background?: string
    dimOverlay?: boolean
  }>(),
  {
    background: '/StartBackground.jpg',
    dimOverlay: false,
  },
)

const emit = defineEmits<{ back: [] }>()
const attrs = useAttrs()
const router = useRouter()

function handleBack() {
  if (attrs.onBack) {
    emit('back')
  } else if (props.previous) {
    router.push(props.previous)
  }
}
</script>

<template>
  <div class="scene">
    <img class="scene-background" :src="background" alt="" aria-hidden="true" />
    <div class="scene-overlay" :class="{ 'scene-overlay--dim': dimOverlay }" />
    <div class="scene-content">
      <NavigationHeader :title="title" :previous="previous" :next="next" @back="handleBack" />
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
  overflow-y: auto;
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

.scene-overlay--dim {
  opacity: 0.8;
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
