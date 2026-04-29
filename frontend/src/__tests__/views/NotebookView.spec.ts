import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import NotebookView from '../../views/NotebookView.vue'

vi.mock('@/components/SceneComponent.vue', () => ({
  default: { name: 'SceneComponent', template: '<div><slot /></div>' },
}))

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({ query: { classroomId: 'classroom-1' } })),
}))

vi.mock('@/generated-client/@tanstack/vue-query.gen', () => ({
  createNotebookMutation: vi.fn(() => ({})),
  getMyNotebooksQueryKey: vi.fn(() => ['getMyNotebooks']),
  getMyNotebooksOptions: vi.fn(() => ({})),
  updateNotebookMutation: vi.fn(() => ({})),
}))

const mockMutateAsync = vi.fn().mockResolvedValue({})
const mockInvalidateQueries = vi.fn()

const mockUseQuery = vi.hoisted(() => vi.fn())
const mockUseMutation = vi.hoisted(() => vi.fn())

vi.mock('@tanstack/vue-query', () => ({
  useQuery: mockUseQuery,
  useMutation: mockUseMutation,
  useQueryClient: vi.fn(() => ({ invalidateQueries: mockInvalidateQueries })),
}))

const mockEntry = {
  id: 'entry-1',
  userId: 'user-1',
  classroomId: 'classroom-1',
  stopLocation: 'nyhetskvartalet',
  autoNotes: '["Tips 1", "Tips 2"]',
  studentReflection: 'Min refleksjon',
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
}

const secondMockEntry = {
  id: 'entry-2',
  userId: 'user-1',
  classroomId: 'classroom-1',
  stopLocation: 'fotografen',
  autoNotes: '["Foto-tips 1"]',
  studentReflection: 'Min andre refleksjon',
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
}

const thirdMockEntry = {
  id: 'entry-3',
  userId: 'user-1',
  classroomId: 'classroom-1',
  stopLocation: 'postkontoret',
  autoNotes: '["E-post tips"]',
  studentReflection: 'Tredje refleksjon',
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
}

function setup(entries = [mockEntry], queryError: Error | null = null, loading = false) {
  mockUseQuery.mockReturnValue({
    data: ref(entries.length ? { data: entries } : { data: [] }),
    isLoading: ref(loading),
    error: ref(queryError),
  })
  mockUseMutation.mockImplementation(() => ({ mutateAsync: mockMutateAsync, mutate: mockMutateAsync }))
  return mount(NotebookView)
}

describe('NotebookView', () => {
  beforeEach(() => {
    mockUseQuery.mockReset()
    mockUseMutation.mockReset()
    mockMutateAsync.mockReset().mockResolvedValue({})
    mockInvalidateQueries.mockReset()
  })

  it('shows loading text while fetching', () => {
    const wrapper = setup([], null, true)
    expect(wrapper.text()).toContain('Laster inn notatblokk...')
  })

  it('shows error message when query fails', () => {
    mockUseQuery.mockReturnValue({
      data: ref(undefined),
      isLoading: ref(false),
      error: ref(new Error('Nettverksfeil')),
    })
    mockUseMutation.mockReturnValue({ mutate: mockMutateAsync, mutateAsync: mockMutateAsync })
    const wrapper = mount(NotebookView)
    expect(wrapper.text()).toContain('Kunne ikke hente notatblokkoppføringer')
  })

  it('shows missing classroom error when classroomId is absent', async () => {
    const { useRoute } = await import('vue-router')
    vi.mocked(useRoute).mockReturnValueOnce({ query: {} } as ReturnType<typeof useRoute>)
    mockUseQuery.mockReturnValue({
      data: ref(undefined),
      isLoading: ref(false),
      error: ref(null),
    })
    mockUseMutation.mockReturnValue({ mutate: mockMutateAsync, mutateAsync: mockMutateAsync })
    const wrapper = mount(NotebookView)
    expect(wrapper.text()).toContain('Mangler klasserom-ID')
  })

  it('shows empty state when no entries exist', () => {
    const wrapper = setup([])
    expect(wrapper.text()).toContain('Du har ikke fullført noen stoppesteder enda')
  })

  it('shows page navigation when entries exist', () => {
    const wrapper = setup()
    expect(wrapper.find('.book-carousel').exists()).toBe(true)
    expect(wrapper.find('.page-indicator').exists()).toBe(true)
  })

  it('unlocked page shows auto notes for matching entry', () => {
    const wrapper = setup()
    expect(wrapper.text()).toContain('Tips 1')
    expect(wrapper.text()).toContain('Tips 2')
  })

  it('shows all stops with locked ones marked', () => {
    const wrapper = setup()
    expect(wrapper.text()).toContain('Nyhetskvartalet')
    expect(wrapper.text()).toContain('Fotografen')
    expect(wrapper.findAll('.locked-content').length).toBeGreaterThan(0)
  })

  it('save button calls mutateAsync with correct payload', async () => {
    const wrapper = setup()
    const saveBtn = wrapper.find('.save-button')
    await saveBtn.trigger('click')
    expect(mockMutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({ stopLocation: 'nyhetskvartalet' }),
    )
  })

  it('prev button is disabled on first spread', () => {
    const wrapper = setup()
    const prevBtn = wrapper.find('[aria-label="Forrige side"]')
    expect(prevBtn.attributes('disabled')).toBeDefined()
  })

  it('next button advances the spread', async () => {
    const wrapper = setup([mockEntry, secondMockEntry, thirdMockEntry])
    const indicator = wrapper.find('.page-indicator')
    expect(indicator.text()).toContain('Side 1 av 4')
    await wrapper.find('[aria-label="Neste side"]').trigger('click')
    expect(indicator.text()).toContain('Side 2 av 4')
  })
})
