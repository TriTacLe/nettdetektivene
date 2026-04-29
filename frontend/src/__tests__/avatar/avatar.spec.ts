import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia } from 'pinia'
import { VueQueryPlugin, QueryClient } from '@tanstack/vue-query'
import { parseAvatarConfig } from '../../utils/avatar'
import { DEFAULT_AVATAR_CONFIG } from '../../types/avatar'
import AvatarEditor from '../../components/avatar/AvatarEditor.vue'

vi.mock('@/components/SceneComponent.vue', () => ({
  default: {
    name: 'SceneComponent',
    template: '<div><slot /></div>',
  },
}))

vi.mock('@dicebear/core', () => ({
  createAvatar: () => ({ toString: () => '<svg></svg>' }),
}))

vi.mock('@dicebear/collection', () => ({
  avataaars: {},
}))

const mockMutate = vi.fn()
const mockPush = vi.fn()

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return { ...actual, useRouter: () => ({ push: mockPush }) }
})

vi.mock('@/generated-client/@tanstack/vue-query.gen.ts', () => ({
  updateProfileMutation: () => ({
    mutationFn: mockMutate,
  }),
  getProfileOptions: () => ({
    queryKey: ['getProfile'],
    queryFn: vi.fn(),
  }),
}))

const router = createRouter({
  history: createWebHashHistory(),
  routes: [{ path: '/', component: { template: '<div />' } }],
})

describe('parseAvatarConfig', () => {
  it('returns defaults on null', () => {
    expect(parseAvatarConfig(null)).toEqual(DEFAULT_AVATAR_CONFIG)
  })

  it('returns defaults on malformed JSON', () => {
    expect(parseAvatarConfig('not json')).toEqual(DEFAULT_AVATAR_CONFIG)
  })

  it('returns defaults on JSON with unknown field values', () => {
    const cfg = { skinColor: 'purple', hair: 'mohawk', hairColor: 'green', hat: 'tophat', clothing: 'armor', prop: 'sword' }
    const result = parseAvatarConfig(JSON.stringify(cfg))
    expect(result.skinColor).toBe(DEFAULT_AVATAR_CONFIG.skinColor)
    expect(result.prop).toBe('none')
  })

  it('parses valid config with prop', () => {
    const cfg = {
      skinColor: 'd08b5b',
      hair: 'straight01',
      hairColor: 'b58143',
      hat: 'hat',
      clothing: 'blazerAndShirt',
      prop: 'round',
    }
    expect(parseAvatarConfig(JSON.stringify(cfg))).toEqual(cfg)
  })

  it('defaults prop when props array field is present instead of prop', () => {
    const legacy = { skinColor: 'edb98a', hair: 'frizzle', hairColor: '2c1b18', hat: 'none', clothing: 'hoodie', props: ['sunglasses', 'round'] }
    const result = parseAvatarConfig(JSON.stringify(legacy))
    expect(result.prop).toBe('none')
  })

  it('handles legacy JSON with old fields (top, pants) gracefully', () => {
    const legacy = { hair: 'long', hairColor: 'red', top: 'hoodie', pants: 'jeans' }
    const result = parseAvatarConfig(JSON.stringify(legacy))
    expect(result.skinColor).toBe(DEFAULT_AVATAR_CONFIG.skinColor)
    expect(result.hair).toBe(DEFAULT_AVATAR_CONFIG.hair)
    expect(result.clothing).toBe(DEFAULT_AVATAR_CONFIG.clothing)
    expect(result.prop).toBe('none')
    expect(result).not.toHaveProperty('top')
    expect(result).not.toHaveProperty('pants')
  })

  it('falls back invalid fields to defaults while keeping valid ones', () => {
    const mixed = { skinColor: 'd08b5b', hair: 'invalid', hairColor: '724133', hat: 'none', clothing: 'hoodie', prop: 'sunglasses' }
    const result = parseAvatarConfig(JSON.stringify(mixed))
    expect(result.skinColor).toBe('d08b5b')
    expect(result.hair).toBe(DEFAULT_AVATAR_CONFIG.hair)
    expect(result.hairColor).toBe('724133')
    expect(result.hat).toBe('none')
    expect(result.clothing).toBe('hoodie')
    expect(result.prop).toBe('sunglasses')
  })
})

describe('AvatarEditor', () => {
  function mountEditor(config = { ...DEFAULT_AVATAR_CONFIG }) {
    return mount(AvatarEditor, {
      props: { modelValue: config },
      global: { plugins: [router] },
    })
  }

  it('emits update:modelValue when a swatch is clicked', async () => {
    const wrapper = mountEditor()
    const unchecked = wrapper.findAll('[role="radio"]').find(
      (r) => r.attributes('aria-checked') === 'false',
    )
    expect(unchecked).toBeTruthy()
    await unchecked!.trigger('click')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
  })

  it('moves selection with ArrowRight', async () => {
    const wrapper = mountEditor()
    await wrapper.find('[role="radiogroup"]').trigger('keydown', { key: 'ArrowRight' })
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
  })

  it('renders sections in exact order [Hudfarge, H\u00e5rfarge, H\u00e5r, Hatt, Kl\u00e6r, Tilbeh\u00f8r]', () => {
    const wrapper = mountEditor()
    const radioLabels = wrapper.findAll('[role="radiogroup"]').map((g) => g.attributes('aria-label'))
    expect(radioLabels).toEqual(['Hudfarge', 'H\u00e5rfarge', 'H\u00e5r', 'Hatt', 'Kl\u00e6r', 'Tilbeh\u00f8r'])
  })

  it('renders 4 options per single-select category', () => {
    const wrapper = mountEditor()
    const groups = wrapper.findAll('[role="radiogroup"]')
    for (const group of groups) {
      expect(group.findAll('[role="radio"]')).toHaveLength(4)
    }
  })

  it('renders detektivutstyr as single-select radiogroup with 4 options', () => {
    const wrapper = mountEditor()
    const propGroup = wrapper.findAll('[role="radiogroup"]').find((g) => g.attributes('aria-label') === 'Tilbeh\u00f8r')
    expect(propGroup).toBeTruthy()
    expect(propGroup!.findAll('[role="radio"]')).toHaveLength(4)
  })

  it('selects prop on click', async () => {
    const wrapper = mountEditor()
    const propGroup = wrapper.findAll('[role="radiogroup"]').find((g) => g.attributes('aria-label') === 'Tilbeh\u00f8r')
    const roundBtn = propGroup!.findAll('[role="radio"]').find((b) => b.attributes('aria-label') === 'Briller')
    await roundBtn!.trigger('click')
    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    const updated = emitted![0][0] as AvatarConfig
    expect(updated.prop).toBe('round')
  })

  it('clears prop when none is clicked', async () => {
    const config = { ...DEFAULT_AVATAR_CONFIG, prop: 'sunglasses' }
    const wrapper = mountEditor(config)
    const propGroup = wrapper.findAll('[role="radiogroup"]').find((g) => g.attributes('aria-label') === 'Tilbeh\u00f8r')
    const noneBtn = propGroup!.findAll('[role="radio"]').find((b) => b.attributes('aria-label') === 'Ingen')
    await noneBtn!.trigger('click')
    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    const updated = emitted![0][0] as AvatarConfig
    expect(updated.prop).toBe('none')
  })

  it('renders hat options including none', () => {
    const wrapper = mountEditor()
    const groups = wrapper.findAll('[role="radiogroup"]')
    const hatGroup = groups.find((g) => g.attributes('aria-label') === 'Hatt')
    expect(hatGroup).toBeTruthy()
    expect(hatGroup!.findAll('[role="radio"]')).toHaveLength(4)
  })
})

describe('SetDisplayNameView', () => {
  beforeEach(() => {
    mockMutate.mockReset()
    mockPush.mockReset()
  })

  it('submits both displayName and avatarConfig in one mutation call', async () => {
    mockMutate.mockResolvedValue({ data: {} })

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    const { default: SetDisplayNameView } = await import(
      '../../views/SetDisplayNameView.vue'
    )

    const wrapper = mount(SetDisplayNameView, {
      global: {
        plugins: [createPinia(), router, [VueQueryPlugin, { queryClient }]],
      },
    })

    await wrapper.find('#display-name').setValue('TestUser')
    const submitBtn = wrapper.findAll('button').find((b) => b.text().includes('Fortsett'))
    expect(submitBtn).toBeTruthy()
    await submitBtn!.trigger('click')
    await flushPromises()

    expect(mockMutate).toHaveBeenCalled()
    const callArgs = mockMutate.mock.calls[0][0]
    expect(callArgs.body).toHaveProperty('displayName', 'TestUser')
    expect(callArgs.body).toHaveProperty('avatarConfig')
    const parsed = JSON.parse(callArgs.body.avatarConfig)
    expect(parsed).toHaveProperty('skinColor')
    expect(parsed).toHaveProperty('hair')
    expect(parsed).toHaveProperty('hairColor')
    expect(parsed).toHaveProperty('hat')
    expect(parsed).toHaveProperty('clothing')
    expect(parsed).toHaveProperty('prop')
    expect(typeof parsed.prop).toBe('string')
  })
})
