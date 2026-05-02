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

  function reset() {
    musicVolume.value = 0.5
    sfxVolume.value = 0.5
    musicMuted.value = false
    sfxMuted.value = false
    textSpeed.value = 45
    autoAdvance.value = false
    sessionStorage.removeItem('audio_musicVolume')
    sessionStorage.removeItem('audio_sfxVolume')
    sessionStorage.removeItem('audio_musicMuted')
    sessionStorage.removeItem('audio_sfxMuted')
    sessionStorage.removeItem('audio_textSpeed')
    sessionStorage.removeItem('audio_autoAdvance')
  }

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

  // One-shot longer audio (e.g. a victory jingle) — uses HTMLAudioElement so
  // playback starts on first call without waiting for a WebAudio decode pass.
  // Only one instance plays at a time. The same-src guard keeps the jingle
  // continuous when navigating between consecutive celebration screens
  // (e.g. task-finished -> stop-finished) instead of restarting on each mount.
  // Background music is ducked (volume → 0) while the jingle plays so the
  // two don't fight. Ducking instead of pausing avoids the play()/pause()
  // promise race that can leave the music audible mid-transition.
  //
  // Declared above play()/setMusicVolume()/toggleMusicMuted() because those
  // functions read musicDuckedForJingle and call stopOneShot(); keeping the
  // declarations co-located with the rest of the music state avoids the TDZ
  // trap a synchronous setup-time call would otherwise spring.
  let oneShotEl: HTMLAudioElement | null = null
  let oneShotSrc: string | null = null
  let musicDuckedForJingle = false

  function unduckMusic() {
    if (!musicDuckedForJingle) return
    musicDuckedForJingle = false
    if (audioEl) audioEl.volume = effectiveMusicVolume()
  }

  function stopOneShot() {
    if (oneShotEl) {
      oneShotEl.pause()
      oneShotEl.src = ''
      oneShotEl = null
      oneShotSrc = null
    }
    unduckMusic()
  }

  function playOneShot(src: string) {
    if (oneShotEl && oneShotSrc === src && !oneShotEl.paused) return
    stopOneShot()
    const vol = effectiveSfxVolume()
    if (vol <= 0) return

    if (audioEl) {
      audioEl.volume = 0
      musicDuckedForJingle = true
    }

    const el = new Audio(src)
    el.volume = vol
    const finish = () => {
      if (oneShotEl === el) {
        oneShotEl = null
        oneShotSrc = null
      }
      unduckMusic()
    }
    el.addEventListener('ended', finish)
    el.addEventListener('error', finish)

    oneShotEl = el
    oneShotSrc = src
    el.play().catch(finish)
  }

  function play(src: string) {
    wantPlaying.value = true
    installUnlockListeners()

    // Same track already loaded — just nudge it to resume if paused. While
    // the music is ducked for the jingle, keep volume at 0 so the two don't
    // play on top of each other; the jingle's end handler will restore it.
    if (currentTrack.value === src && audioEl) {
      audioEl.volume = musicDuckedForJingle ? 0 : effectiveMusicVolume()
      if (audioEl.paused) tryResume()
      return
    }

    // Different track: tear down old, build new. The jingle (if any) is tied
    // to the previous music context — stop it so it doesn't outlive the swap.
    stopOneShot()
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
    if (audioEl && !musicDuckedForJingle) audioEl.volume = effectiveMusicVolume()
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
      if (!musicDuckedForJingle) audioEl.volume = effectiveMusicVolume()
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

  // High-frequency SFX (typing ticks fire ~45x/sec) is far too heavy for
  // HTMLAudioElement.play() — each call has its own decode/start latency and
  // GC pressure. WebAudio decodes once into a shared AudioBuffer and spawns
  // cheap AudioBufferSourceNodes per play.
  let sfxContext: AudioContext | null = null
  const sfxBuffers = new Map<string, AudioBuffer>()
  const sfxLoading = new Set<string>()

  function ensureSfxContext(): AudioContext | null {
    if (typeof window === 'undefined') return null
    const Ctor = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!Ctor) return null
    if (!sfxContext) sfxContext = new Ctor()
    return sfxContext
  }

  async function loadSfxBuffer(src: string, ctx: AudioContext) {
    if (sfxBuffers.has(src) || sfxLoading.has(src)) return
    sfxLoading.add(src)
    try {
      const res = await fetch(src)
      const arr = await res.arrayBuffer()
      const buf = await ctx.decodeAudioData(arr)
      sfxBuffers.set(src, buf)
    } catch {
      // Ignore — playSfx will silently no-op until the buffer loads next time.
    } finally {
      sfxLoading.delete(src)
    }
  }

  function playSfx(src: string) {
    const vol = effectiveSfxVolume()
    if (vol <= 0) return
    const ctx = ensureSfxContext()
    if (!ctx) return
    if (ctx.state === 'suspended') ctx.resume().catch(() => {})
    const buf = sfxBuffers.get(src)
    if (!buf) {
      loadSfxBuffer(src, ctx).catch(() => {})
      return
    }
    const source = ctx.createBufferSource()
    source.buffer = buf
    const gain = ctx.createGain()
    gain.gain.value = vol
    source.connect(gain).connect(ctx.destination)
    source.start(0)
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
    reset,
    playSfx,
    playTypingTick,
    playOneShot,
    stopOneShot,
  }
})
