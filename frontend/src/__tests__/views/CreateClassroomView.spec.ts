import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import CreateClassroomView from '../../views/teacher/CreateClassroomView.vue'

vi.mock('@/components/SceneComponent.vue', () => ({
  default: { name: 'SceneComponent', template: '<div><slot /></div>' },
}))

vi.mock('@/generated-client/@tanstack/vue-query.gen.ts', () => ({
  createClassroomMutation: vi.fn(() => ({})),
  updateClassroomMutation: vi.fn(() => ({})),
  getAllInstitutionsOptions: vi.fn(() => ({})),
  getClassroomOptions: vi.fn(() => ({})),
}))

const mockPush = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({ push: mockPush }),
    useRoute: () => ({ params: {}, query: {} }),
  }
})

const mockMutate = vi.fn()
const mockUseQuery = vi.hoisted(() => vi.fn())
const mockUseMutation = vi.hoisted(() => vi.fn())

vi.mock('@tanstack/vue-query', () => ({
  useQuery: mockUseQuery,
  useMutation: mockUseMutation,
}))

const institutions = [
  { id: 'inst-1', instName: 'Testskolen' },
  { id: 'inst-2', instName: 'Annen skole' },
]

function setup(institutionList = institutions) {
  mockUseQuery
    .mockReturnValueOnce({ data: ref({ data: institutionList }), isLoading: ref(false) })
    .mockReturnValueOnce({ data: ref(null), isLoading: ref(false) })
  mockUseMutation.mockReturnValue({ mutate: mockMutate, isPending: ref(false) })
  return mount(CreateClassroomView)
}

describe('CreateClassroomView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the create classroom heading', () => {
    const wrapper = setup()
    expect(wrapper.text()).toContain('Lag Klasserom')
  })

  it('renders title input field', () => {
    const wrapper = setup()
    expect(wrapper.find('#classroom-title').exists()).toBe(true)
  })

  it('renders description textarea', () => {
    const wrapper = setup()
    expect(wrapper.find('#classroom-desc').exists()).toBe(true)
  })

  it('renders institution select with options', () => {
    const wrapper = setup()
    const select = wrapper.find('#classroom-institution')
    expect(select.exists()).toBe(true)
    expect(wrapper.text()).toContain('Testskolen')
    expect(wrapper.text()).toContain('Annen skole')
  })

  it('renders game enable checkbox', () => {
    const wrapper = setup()
    expect(wrapper.find('input[type="checkbox"]').exists()).toBe(true)
  })

  it('renders submit button', () => {
    const wrapper = setup()
    expect(wrapper.find('button[type="submit"]').exists()).toBe(true)
    expect(wrapper.find('button[type="submit"]').text()).toContain('Opprett klasserom')
  })

  it('shows title validation error when submitting empty form', async () => {
    const wrapper = setup()
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Navn på klasserom kreves')
  })

  it('shows institution validation error when no institution selected', async () => {
    const wrapper = setup()
    await wrapper.find('#classroom-title').setValue('5A')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Velg en institusjon')
  })

  it('does not show errors before submit', () => {
    const wrapper = setup()
    expect(wrapper.text()).not.toContain('Navn på klasserom kreves')
    expect(wrapper.text()).not.toContain('Velg en institusjon')
  })

  it('calls createMutation when form is valid', async () => {
    const wrapper = setup()
    await wrapper.find('#classroom-title').setValue('5A')
    await wrapper.find('#classroom-institution').setValue('inst-1')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(mockMutate).toHaveBeenCalled()
  })

  it('shows loading text on submit button while pending', () => {
    mockUseQuery
      .mockReturnValueOnce({ data: ref({ data: institutions }), isLoading: ref(false) })
      .mockReturnValueOnce({ data: ref(null), isLoading: ref(false) })
    mockUseMutation.mockReturnValue({ mutate: mockMutate, isPending: ref(true) })
    const wrapper = mount(CreateClassroomView)
    expect(wrapper.find('button[type="submit"]').text()).toContain('Oppretter...')
  })

  it('shows "Laster institusjoner..." while loading institutions', () => {
    mockUseQuery
      .mockReturnValueOnce({ data: ref(null), isLoading: ref(true) })
      .mockReturnValueOnce({ data: ref(null), isLoading: ref(false) })
    mockUseMutation.mockReturnValue({ mutate: mockMutate, isPending: ref(false) })
    const wrapper = mount(CreateClassroomView)
    expect(wrapper.text()).toContain('Laster institusjoner')
  })

  it('shows error when no institutions found', () => {
    mockUseQuery
      .mockReturnValueOnce({ data: ref({ data: [] }), isLoading: ref(false) })
      .mockReturnValueOnce({ data: ref(null), isLoading: ref(false) })
    mockUseMutation.mockReturnValue({ mutate: mockMutate, isPending: ref(false) })
    const wrapper = mount(CreateClassroomView)
    expect(wrapper.text()).toContain('Ingen institusjoner funnet')
  })
})
