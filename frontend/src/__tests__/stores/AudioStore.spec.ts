import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAudioStore } from '../../stores/AudioStore'

describe('AudioStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
  })

  it('initializes with default values', () => {
    const store = useAudioStore()
    expect(store.musicVolume).toBe(0.5)
    expect(store.sfxVolume).toBe(0.5)
    expect(store.musicMuted).toBe(false)
    expect(store.sfxMuted).toBe(false)
  })

  it('setMusicVolume updates value and sessionStorage', () => {
    const store = useAudioStore()
    store.setMusicVolume(0.8)
    expect(store.musicVolume).toBe(0.8)
    expect(sessionStorage.getItem('audio_musicVolume')).toBe('0.8')
  })

  it('setSfxVolume updates value and sessionStorage', () => {
    const store = useAudioStore()
    store.setSfxVolume(0.3)
    expect(store.sfxVolume).toBe(0.3)
    expect(sessionStorage.getItem('audio_sfxVolume')).toBe('0.3')
  })

  it('toggleMusicMuted flips muted state', () => {
    const store = useAudioStore()
    store.toggleMusicMuted()
    expect(store.musicMuted).toBe(true)
    store.toggleMusicMuted()
    expect(store.musicMuted).toBe(false)
  })

  it('toggleSfxMuted flips muted state', () => {
    const store = useAudioStore()
    store.toggleSfxMuted()
    expect(store.sfxMuted).toBe(true)
    store.toggleSfxMuted()
    expect(store.sfxMuted).toBe(false)
  })

  it('persists muted state to sessionStorage', () => {
    const store = useAudioStore()
    store.toggleMusicMuted()
    expect(sessionStorage.getItem('audio_musicMuted')).toBe('true')
  })
})
