<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAudioStore } from '@/stores/AudioStore'
import Toast from '@/components/Toast.vue'
import { useToast } from '@/composables/UseToast'

const route = useRoute()
const audioStore = useAudioStore()
const { toast } = useToast()

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
  <RouterView v-slot="{ Component }">
    <Transition name="scene-fade" mode="out-in">
      <component :is="Component" />
    </Transition>
  </RouterView>
  <Toast v-bind="toast" />
</template>
