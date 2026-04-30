<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavigationHeader from './NavigationHeader.vue'

const props = withDefaults(
  defineProps<{
    title: string
    previous?: string
    next?: string
    background?: string
    backgroundMobile?: string
    vignette?: boolean
    scanlines?: boolean
    dimOverlay?: boolean
    overlayOpacity?: number
  }>(),
  {
    background: '/StartBackground.jpg',
    vignette: true,
    scanlines: false,
    dimOverlay: false,
  },
)

const emit = defineEmits<{ back: [] }>()
const attrs = useAttrs()
const route = useRoute()
const router = useRouter()

// Background priority: explicit prop > route meta > default
const resolvedBackground = computed(() => {
  if (props.background) return props.background
  const metaBg = route.meta.background as string | undefined
  return metaBg ?? '/StartBackground.jpg'
})

const resolvedScanlines = computed(() => {
  if (props.scanlines) return true
  return Boolean(route.meta.scanlines)
})

function handleBack() {
  if (attrs.onBack) {
    emit('back')
  } else if (props.previous) {
    router.push(props.previous)
  }
}
</script>

<template>
  <div class="scene" :class="{ scanlines: resolvedScanlines }">
    <template v-if="backgroundMobile">
      <img
        class="scene-background scene-background--desktop"
        :src="resolvedBackground"
        alt=""
        aria-hidden="true"
      />
      <img
        class="scene-background scene-background--mobile"
        :src="backgroundMobile"
        alt=""
        aria-hidden="true"
      />
    </template>
    <img v-else class="scene-background" :src="resolvedBackground" alt="" aria-hidden="true" />
    <div
      class="scene-overlay"
      :class="{ 'scene-overlay--dim': dimOverlay }"
      :style="overlayOpacity !== undefined ? { opacity: overlayOpacity } : {}"
    ></div>
    <div v-if="vignette" class="scene-vignette" aria-hidden="true"></div>
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
  display: flex;
  flex-direction: column;
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

.scene-vignette {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 11;
  background: radial-gradient(
    ellipse at center,
    rgba(0, 0, 0, 0) 40%,
    var(--color-nd-vignette, rgba(0, 0, 0, 0.55)) 100%
  );
}

.scene-content {
  position: relative;
  z-index: 20;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.scene-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.scene-background--mobile {
  display: none;
}

@media (max-aspect-ratio: 1/1) {
  .scene-background--desktop {
    display: none;
  }
  .scene-background--mobile {
    display: block;
  }
}
</style>
