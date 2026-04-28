<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAudioStore } from '@/stores/AudioStore'

const route = useRoute()
const audioStore = useAudioStore()

watch(
  () => route.meta.music,
  (music) => {
    const track = music === 'dark' ? '/music/dark.mp3' : '/music/main.mp3'
    audioStore.play(track)
  },
  { immediate: true },
)
</script>

<template>
  <RouterView />
</template>
