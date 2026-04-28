import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useAudioStore = defineStore('audio', () => {
  const musicVolume = ref(parseFloat(sessionStorage.getItem('audio_musicVolume') ?? '0.5'))
  const sfxVolume = ref(parseFloat(sessionStorage.getItem('audio_sfxVolume') ?? '0.5'))
  const musicMuted = ref(sessionStorage.getItem('audio_musicMuted') === 'true')
  const sfxMuted = ref(sessionStorage.getItem('audio_sfxMuted') === 'true')

  function setMusicVolume(volume: number) {
    musicVolume.value = volume
    sessionStorage.setItem('audio_musicVolume', String(volume))
  }

  function setSfxVolume(volume: number) {
    sfxVolume.value = volume
    sessionStorage.setItem('audio_sfxVolume', String(volume))
  }

  function toggleMusicMuted() {
    musicMuted.value = !musicMuted.value
    sessionStorage.setItem('audio_musicMuted', String(musicMuted.value))
  }

  function toggleSfxMuted() {
    sfxMuted.value = !sfxMuted.value
    sessionStorage.setItem('audio_sfxMuted', String(sfxMuted.value))
  }

  return { musicVolume, sfxVolume, musicMuted, sfxMuted, setMusicVolume, setSfxVolume, toggleMusicMuted, toggleSfxMuted }
})
