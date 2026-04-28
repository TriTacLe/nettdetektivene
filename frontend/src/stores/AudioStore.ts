import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useAudioStore = defineStore('audio', () => {
  const musicVolume = ref(parseFloat(sessionStorage.getItem('audio_musicVolume') ?? '0.5'))
  const sfxVolume = ref(parseFloat(sessionStorage.getItem('audio_sfxVolume') ?? '0.5'))
  const musicMuted = ref(sessionStorage.getItem('audio_musicMuted') === 'true')
  const sfxMuted = ref(sessionStorage.getItem('audio_sfxMuted') === 'true')

  const currentTrack = ref<string | null>(null)
  let audioEl: HTMLAudioElement | null = null

  function effectiveVolume() {
    return musicMuted.value ? 0 : musicVolume.value
  }

  function play(src: string) {
    if (currentTrack.value === src && audioEl && !audioEl.paused) return

    if (audioEl) {
      audioEl.pause()
      audioEl.src = ''
    }

    const el = new Audio(src)
    el.loop = true
    el.volume = effectiveVolume()
    el.play().catch(() => {})
    audioEl = el
    currentTrack.value = src
  }

  function stop() {
    if (audioEl) {
      audioEl.pause()
      audioEl.src = ''
      audioEl = null
    }
    currentTrack.value = null
  }

  function setMusicVolume(volume: number) {
    musicVolume.value = volume
    sessionStorage.setItem('audio_musicVolume', String(volume))
    if (audioEl) audioEl.volume = effectiveVolume()
  }

  function setSfxVolume(volume: number) {
    sfxVolume.value = volume
    sessionStorage.setItem('audio_sfxVolume', String(volume))
  }

  function toggleMusicMuted() {
    musicMuted.value = !musicMuted.value
    sessionStorage.setItem('audio_musicMuted', String(musicMuted.value))
    if (audioEl) audioEl.volume = effectiveVolume()
  }

  function toggleSfxMuted() {
    sfxMuted.value = !sfxMuted.value
    sessionStorage.setItem('audio_sfxMuted', String(sfxMuted.value))
  }

  return {
    musicVolume,
    sfxVolume,
    musicMuted,
    sfxMuted,
    currentTrack,
    play,
    stop,
    setMusicVolume,
    setSfxVolume,
    toggleMusicMuted,
    toggleSfxMuted,
  }
})
