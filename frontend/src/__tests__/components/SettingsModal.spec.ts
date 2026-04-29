import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import SettingsModal from '../../components/SettingsModal.vue'


const mockLogout = vi.fn()
vi.mock('../../stores/AuthStore', () => ({
  useAuthStore: () => ({ logout: mockLogout }),
}))

vi.mock('../../stores/AudioStore', () => ({
  useAudioStore: () => ({
    musicVolume: 0.5,
    sfxVolume: 0.5,
    musicMuted: false,
    sfxMuted: false,
    textSpeed: 50,
    autoAdvance: false,
    setMusicVolume: vi.fn(),
    setSfxVolume: vi.fn(),
    toggleMusicMuted: vi.fn(),
    toggleSfxMuted: vi.fn(),
    setTextSpeed: vi.fn(),
    toggleAutoAdvance: vi.fn(),
  }),
}))

describe('SettingsModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockLogout.mockReset()
  })

  it('renders music and sfx controls', () => {
    const wrapper = mount(SettingsModal)
    expect(wrapper.text()).toContain('Musikk')
    expect(wrapper.text()).toContain('Lydeffekter')
  })

  it('emits close when backdrop is clicked', async () => {
    const wrapper = mount(SettingsModal)
    await wrapper.find('.modal-backdrop').trigger('click')
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('emits close when close button is clicked', async () => {
    const wrapper = mount(SettingsModal)
    await wrapper.find('.close-button').trigger('click')
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('calls logout on logout button click', async () => {
    const wrapper = mount(SettingsModal)
    await wrapper.find('.btn-vn-danger').trigger('click')
    expect(mockLogout).toHaveBeenCalled()
  })

  it('renders volume sliders', () => {
    const wrapper = mount(SettingsModal)
    const sliders = wrapper.findAll('input[type="range"]')
    expect(sliders).toHaveLength(3)
  })
})
