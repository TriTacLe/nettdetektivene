import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import AchievementsView from '../../views/AchievementsView.vue'

vi.mock('@/components/SceneComponent.vue', () => ({
  default: { name: 'SceneComponent', template: '<div><slot /></div>' },
}))

vi.mock('@/components/BadgeCard.vue', () => ({
  default: {
    name: 'BadgeCard',
    props: ['title', 'description', 'unlocked'],
    template: '<div class="badge-card" :data-unlocked="String(unlocked)">{{ title }}</div>',
  },
}))

vi.mock('@/generated-client/@tanstack/vue-query.gen', () => ({
  getProfileOptions: vi.fn(() => ({})),
  getAllAchievementsOptions: vi.fn(() => ({})),
  getMyAchievementsOptions: vi.fn(() => ({})),
}))

const mockUseQuery = vi.hoisted(() => vi.fn())
vi.mock('@tanstack/vue-query', () => ({ useQuery: mockUseQuery }))

function setup() {
  mockUseQuery
    .mockReturnValueOnce({ data: ref({ data: { displayName: 'Tri', xp: 100, xpLevel: 2 } }) })
    .mockReturnValueOnce({
      data: ref({
        data: [
          { id: 'ach1', title: 'Nybegynner detektiv', descAch: 'Du har nådd level 2' },
          { id: 'ach2', title: 'Erfaren detektiv', descAch: 'Du har nådd level 5' },
          { id: 'ach3', title: 'Perfeksjonist', descAch: 'Fullfør uten feil' },
        ],
      }),
    })
    .mockReturnValueOnce({ data: ref({ data: [{ achievementId: 'ach1' }] }) })

  return mount(AchievementsView)
}

describe('AchievementsView', () => {
  beforeEach(() => mockUseQuery.mockReset())

  it('renders profile displayName, xpLevel and xp', () => {
    const wrapper = setup()
    expect(wrapper.text()).toContain('Tri')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('100')
  })

  it('renders earned / total progress', () => {
    const wrapper = setup()
    expect(wrapper.text()).toContain('1 / 3')
  })

  it('renders correct percentage', () => {
    const wrapper = setup()
    expect(wrapper.text()).toContain('33%')
  })

  it('renders one badge card per achievement', () => {
    const wrapper = setup()
    expect(wrapper.findAll('.badge-card')).toHaveLength(3)
  })

  it('marks earned badge as unlocked', () => {
    const wrapper = setup()
    expect(wrapper.findAll('.badge-card')[0].attributes('data-unlocked')).toBe('true')
  })

  it('marks unearned badges as locked', () => {
    const wrapper = setup()
    expect(wrapper.findAll('.badge-card')[1].attributes('data-unlocked')).toBe('false')
    expect(wrapper.findAll('.badge-card')[2].attributes('data-unlocked')).toBe('false')
  })

  it('renders empty badge list when no achievements exist', () => {
    mockUseQuery
      .mockReturnValueOnce({ data: ref({ data: { displayName: 'Tri', xp: 0, xpLevel: 1 } }) })
      .mockReturnValueOnce({ data: ref({ data: [] }) })
      .mockReturnValueOnce({ data: ref({ data: [] }) })

    const wrapper = mount(AchievementsView)
    expect(wrapper.findAll('.badge-card')).toHaveLength(0)
    expect(wrapper.text()).toContain('0 / 0')
  })
})
