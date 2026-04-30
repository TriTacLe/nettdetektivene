<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAudioStore } from '@/stores/AudioStore'
import { useAuthStore } from '@/stores/AuthStore'
import { useStopStore } from '@/stores/StopStore'
import Toast from '@/components/ToastMessage.vue'
import { useToast } from '@/composables/UseToast'

const route = useRoute()
const audioStore = useAudioStore()
const authStore = useAuthStore()
const stopStore = useStopStore()
const { toast } = useToast()

// Warm-boot: if the persisted auth survives a tab reload, fetch this
// user's completed task IDs from the backend so the map renders the
// correct stars without ever consulting sessionStorage for completion.
if (authStore.isAuthenticated) {
  stopStore.loadCompleted()
}

// Per-track music. Each stop has its own loop; everything else gets `main`.
const MUSIC_TRACKS: Record<string, string> = {
  main:             '/music/main.mp3',
  nyhetskvartalet:  '/music/nyhetskvartalet.mp3',
  fotografen:       '/music/fotografen.mp3',
  datasenteret:     '/music/datasenteret.mp3',
  mystery:          '/music/mystery.mp3',
}

const DEFAULT_TRACK = '/music/main.mp3'

function pickTrack(): string {
  // /stops/<slug>(/...)  — covers both the stop list view and task views.
  const slug = route.path.match(/^\/stops\/([^/]+)/)?.[1]
  if (slug && MUSIC_TRACKS[slug]) return MUSIC_TRACKS[slug]

  // Post-task screens (task-finished, task-failed, stop-finished) carry
  // ?stop=<slug> in the query so we can keep playing that stop's music.
  const queryStop = route.query.stop
  if (typeof queryStop === 'string' && MUSIC_TRACKS[queryStop]) return MUSIC_TRACKS[queryStop]

  // Allow any route to opt into a specific track via meta.music = <slug>.
  const metaKey = route.meta.music
  if (typeof metaKey === 'string' && MUSIC_TRACKS[metaKey]) return MUSIC_TRACKS[metaKey]

  return DEFAULT_TRACK
}

watch(
  () => route.fullPath,
  () => audioStore.play(pickTrack()),
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
