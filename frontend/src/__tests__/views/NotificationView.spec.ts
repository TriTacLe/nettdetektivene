import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import NotificationView from '../../views/teacher/NotificationView.vue'

vi.mock('@/components/SceneComponent.vue', () => ({
  default: { name: 'SceneComponent', template: '<div><slot /></div>' },
}))

vi.mock('@/generated-client/@tanstack/vue-query.gen', () => ({
  getMyNotificationsOptions: vi.fn(() => ({})),
  getMyNotificationsQueryKey: vi.fn(() => ['notifications']),
  getAllClassroomsOptions: vi.fn(() => ({})),
  markAsReadMutation: vi.fn(() => ({})),
  approveMutation: vi.fn(() => ({})),
  removeMutation: vi.fn(() => ({})),
  approveNameChangeMutation: vi.fn(() => ({})),
  rejectNameChangeMutation: vi.fn(() => ({})),
}))

const mockPush = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({ push: mockPush }),
    useRoute: () => ({ query: {} }),
  }
})

const mockMutate = vi.fn()
const mockUseQuery = vi.hoisted(() => vi.fn())
const mockUseMutation = vi.hoisted(() => vi.fn())
const mockUseQueryClient = vi.hoisted(() => vi.fn(() => ({ invalidateQueries: vi.fn() })))

vi.mock('@tanstack/vue-query', () => ({
  useQuery: mockUseQuery,
  useMutation: mockUseMutation,
  useQueryClient: mockUseQueryClient,
}))

const makeNotification = (overrides = {}) => ({
  id: 'n1',
  classroomId: 'c1',
  refId: 'u1',
  msg: 'Testmelding',
  createdAt: new Date().toISOString(),
  isRead: false,
  refType: 'stop_completed',
  ...overrides,
})

function setupMocks(notifications: object[] = [], classrooms: object[] = []) {
  mockUseMutation.mockReturnValue({ mutate: mockMutate, isPending: ref(false) })
  mockUseQuery
    .mockReturnValueOnce({
      data: ref({ data: notifications }),
      isLoading: ref(false),
      error: ref(null),
    })
    .mockReturnValueOnce({
      data: ref({ data: classrooms }),
      isLoading: ref(false),
      error: ref(null),
    })
}

describe('NotificationView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders loading state', () => {
    mockUseMutation.mockReturnValue({ mutate: mockMutate, isPending: ref(false) })
    mockUseQuery
      .mockReturnValueOnce({ data: ref(null), isLoading: ref(true), error: ref(null) })
      .mockReturnValueOnce({ data: ref(null), isLoading: ref(false), error: ref(null) })

    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Laster varsler')
  })

  it('renders error state', () => {
    mockUseMutation.mockReturnValue({ mutate: mockMutate, isPending: ref(false) })
    mockUseQuery
      .mockReturnValueOnce({
        data: ref(null),
        isLoading: ref(false),
        error: ref(new Error('fail')),
      })
      .mockReturnValueOnce({ data: ref(null), isLoading: ref(false), error: ref(null) })

    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Kunne ikke hente varsler')
  })

  it('renders empty state when no notifications', () => {
    setupMocks([])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Ingen varsler')
  })

  it('renders a list of notifications', () => {
    setupMocks([makeNotification(), makeNotification({ id: 'n2', msg: 'Andre melding' })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Testmelding')
    expect(wrapper.text()).toContain('Andre melding')
  })

  it('shows "Ny" badge for unread notifications', () => {
    setupMocks([makeNotification({ isRead: false })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Ny')
  })

  it('does not show "Ny" badge for read notifications', () => {
    setupMocks([makeNotification({ isRead: true })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).not.toContain('Ny')
  })

  it('renders approve/reject buttons for JOIN_REQUEST', () => {
    setupMocks([makeNotification({ refType: 'classroom_student', isRead: false })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Godkjenn')
    expect(wrapper.text()).toContain('Avvis')
  })

  it('renders approve/reject buttons for NAME_CHANGE', () => {
    setupMocks([makeNotification({ refType: 'name_change', isRead: false })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Godkjenn')
    expect(wrapper.text()).toContain('Avvis')
  })

  it('renders "Dempe" button for non-actionable unread notifications', () => {
    setupMocks([makeNotification({ refType: 'stop_completed', isRead: false })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Dempe')
  })

  it('renders "Demp alle" button when there are non-actionable notifications', () => {
    setupMocks([makeNotification({ refType: 'stop_completed', isRead: false })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Demp alle')
  })

  it('renders filter select for type', () => {
    setupMocks([])
    const wrapper = mount(NotificationView)
    expect(wrapper.find('select').exists()).toBe(true)
  })

  it('renders notification type label for STOP_COMPLETED', () => {
    setupMocks([makeNotification({ refType: 'stop_completed' })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Område fullført')
  })

  it('renders notification type label for MYSTERY_SUBMITTED', () => {
    setupMocks([makeNotification({ refType: 'mystery_submission' })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Ukens mysterium')
  })

  it('renders notification type label for STUCK', () => {
    setupMocks([makeNotification({ refType: 'stuck' })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Elev står fast')
  })

  it('renders notification type label for INAPPROPRIATE_MESSAGE', () => {
    setupMocks([makeNotification({ refType: 'inappropriate_message' })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Upassende tekst')
  })

  it('renders notification type label for FAILED', () => {
    setupMocks([makeNotification({ refType: 'task_attempt_failed' })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Feilet')
  })

  it('renders notification type label for CORRECT', () => {
    setupMocks([makeNotification({ refType: 'task_attempt_correct' })])
    const wrapper = mount(NotificationView)
    expect(wrapper.text()).toContain('Riktig')
  })

  it('shows classroom filter select when no classroomId in route', () => {
    setupMocks([makeNotification()])
    const wrapper = mount(NotificationView)
    const selects = wrapper.findAll('select')
    expect(selects.length).toBeGreaterThanOrEqual(1)
  })
})
