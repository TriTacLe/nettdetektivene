import { ref } from 'vue'
import { defineStore } from 'pinia'

const SFX_TYPING = '/sfx/typing.mp3'

export const useAudioStore = defineStore('audio', () => {
  const musicVolume = ref(parseFloat(sessionStorage.getItem('audio_musicVolume') ?? '0.5'))
  const sfxVolume = ref(parseFloat(sessionStorage.getItem('audio_sfxVolume') ?? '0.5'))
  const musicMuted = ref(sessionStorage.getItem('audio_musicMuted') === 'true')
  const sfxMuted = ref(sessionStorage.getItem('audio_sfxMuted') === 'true')

  // Dialog/text preferences. Persisted alongside audio so SettingsModal owns
  // the whole "experience" panel. textSpeed is characters-per-second.
  const textSpeed = ref(parseInt(sessionStorage.getItem('audio_textSpeed') ?? '45', 10))
  const autoAdvance = ref(sessionStorage.getItem('audio_autoAdvance') === 'true')

  const currentTrack = ref<string | null>(null)
  // wantPlaying = "we believe music should currently be playing". Distinct
  // from audioEl.paused, which can flip silently due to autoplay blocking,
  // tab backgrounding, OS interruptions, etc.
  const wantPlaying = ref(false)
  let audioEl: HTMLAudioElement | null = null
  let unlockListenersInstalled = false

  function effectiveMusicVolume() {
    return musicMuted.value ? 0 : musicVolume.value
  }

  function effectiveSfxVolume() {
    return sfxMuted.value ? 0 : sfxVolume.value
  }

  function tryResume() {
    if (!wantPlaying.value || !audioEl || !audioEl.paused) return
    audioEl.play().catch(() => {
      // Still blocked — will retry on next user interaction.
    })
  }

  // Register listeners exactly once. They fire across the lifetime of the
  // store regardless of how many times play() is called.
  function installUnlockListeners() {
    if (unlockListenersInstalled || typeof document === 'undefined') return
    unlockListenersInstalled = true

    const onInteract = () => tryResume()
    document.addEventListener('pointerdown', onInteract)
    document.addEventListener('keydown', onInteract)
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) tryResume()
    })
    window.addEventListener('focus', () => tryResume())
    window.addEventListener('pageshow', () => tryResume())
  }

  function play(src: string) {
    wantPlaying.value = true
    installUnlockListeners()

    // Same track already loaded — just nudge it to resume if paused.
    if (currentTrack.value === src && audioEl) {
      audioEl.volume = effectiveMusicVolume()
      if (audioEl.paused) tryResume()
      return
    }

    // Different track: tear down old, build new.
    if (audioEl) {
      audioEl.pause()
      audioEl.src = ''
    }

    const el = new Audio(src)
    el.loop = true
    el.volume = effectiveMusicVolume()
    // If the browser pauses us (autoplay block, OS interrupt), keep our
    // intent state but rely on tryResume() via interaction/visibility.
    el.addEventListener('pause', () => {
      // Intentionally a no-op here — tryResume() is driven by user gestures
      // and visibility events, not by the pause event itself, to avoid a
      // tight pause/play loop when the browser is actively blocking us.
    })
    audioEl = el
    currentTrack.value = src
    tryResume()
  }

  function stop() {
    wantPlaying.value = false
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
    if (audioEl) audioEl.volume = effectiveMusicVolume()
  }

  function setSfxVolume(volume: number) {
    sfxVolume.value = volume
    sessionStorage.setItem('audio_sfxVolume', String(volume))
  }

  function toggleMusicMuted() {
    musicMuted.value = !musicMuted.value
    sessionStorage.setItem('audio_musicMuted', String(musicMuted.value))
    // Mute by zeroing volume so the audio element keeps "playing" — that way
    // unmute is instant and we don't need to re-trigger autoplay unlock.
    if (audioEl) {
      audioEl.volume = effectiveMusicVolume()
      if (!musicMuted.value) tryResume()
    }
  }

  function toggleSfxMuted() {
    sfxMuted.value = !sfxMuted.value
    sessionStorage.setItem('audio_sfxMuted', String(sfxMuted.value))
  }

  function setTextSpeed(speed: number) {
    textSpeed.value = speed
    sessionStorage.setItem('audio_textSpeed', String(speed))
  }

  function toggleAutoAdvance() {
    autoAdvance.value = !autoAdvance.value
    sessionStorage.setItem('audio_autoAdvance', String(autoAdvance.value))
  }

  // Fire-and-forget short SFX. Each call gets its own Audio element so
  // overlapping ticks don't cancel each other.
  function playSfx(src: string) {
    const vol = effectiveSfxVolume()
    if (vol <= 0) return
    const el = new Audio(src)
    el.volume = vol
    el.play().catch(() => {})
  }

  function playTypingTick() {
    playSfx(SFX_TYPING)
  }

  return {
    musicVolume,
    sfxVolume,
    musicMuted,
    sfxMuted,
    textSpeed,
    autoAdvance,
    currentTrack,
    wantPlaying,
    play,
    stop,
    setMusicVolume,
    setSfxVolume,
    toggleMusicMuted,
    toggleSfxMuted,
    setTextSpeed,
    toggleAutoAdvance,
    playSfx,
    playTypingTick,
  }
})
