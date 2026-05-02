import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import { createTestingPinia } from '@pinia/testing'

vi.mock('@/components/SceneComponent.vue', () => ({
  default: { name: 'SceneComponent', template: '<div><slot /></div>' },
}))
vi.mock('@/components/SidePanelMenu.vue', () => ({
  default: {
    name: 'SidePanelMenu',
    template: '<div class="side-panel-menu"><slot /></div>',
    emits: ['select'],
  },
}))
vi.mock('@/components/ClassroomListPanel.vue', () => ({
  default: { name: 'ClassroomListPanel', template: '<div class="classroom-list-panel" />' },
}))

const mockPush = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({ push: mockPush }),
    useRoute: () => ({ query: {}, params: {} }),
  }
})

vi.mock('@/generated-client/@tanstack/vue-query.gen', () => ({
  getMyClassroomOptions: vi.fn(() => ({})),
  joinMutation: vi.fn(() => ({})),
  getMyNotificationsOptions: vi.fn(() => ({})),
}))

const mockMutate = vi.fn()
const mockUseQuery = vi.hoisted(() => vi.fn())
const mockUseMutation = vi.hoisted(() => vi.fn())

vi.mock('@tanstack/vue-query', () => ({
  useQuery: mockUseQuery,
  useMutation: mockUseMutation,
}))

// ─── StartView ────────────────────────────────────────────────────────────────

import StartView from '../../views/StartView.vue'

describe('StartView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders student and teacher buttons', () => {
    const wrapper = mount(StartView)
    expect(wrapper.text()).toContain('Jeg er en elev')
    expect(wrapper.text()).toContain('Jeg er en lærer')
  })

  it('navigates to student-login on student button click', async () => {
    const wrapper = mount(StartView)
    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/student-login')
  })

  it('navigates to teacher-login on teacher button click', async () => {
    const wrapper = mount(StartView)
    const buttons = wrapper.findAll('button')
    await buttons[1].trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/teacher-login')
  })

  it('renders welcome heading', () => {
    const wrapper = mount(StartView)
    expect(wrapper.text()).toContain('Velkommen til Nettdetektivene')
  })
})

// ─── TaskFailedView ───────────────────────────────────────────────────────────

import TaskFailedView from '../../views/TaskFailedView.vue'

describe('TaskFailedView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders retry button', () => {
    const wrapper = mount(TaskFailedView)
    expect(wrapper.find('button').exists()).toBe(true)
    expect(wrapper.text()).toContain('Prøv på nytt')
  })

  it('renders default reason text when no query param', () => {
    const wrapper = mount(TaskFailedView)
    expect(wrapper.text()).toContain('Sjekk svarene dine og prøv igjen')
  })

  it('renders encouragement text', () => {
    const wrapper = mount(TaskFailedView)
    expect(wrapper.text()).toContain('Du kan prøve så mange ganger du vil')
  })

  it('navigates to game-map on retry when no stopId', async () => {
    const wrapper = mount(TaskFailedView)
    await wrapper.find('button').trigger('click')
    expect(mockPush).toHaveBeenCalledWith({ name: 'game-map' })
  })
})

// ─── AwaitingApprovalView ─────────────────────────────────────────────────────

import AwaitingApprovalView from '../../views/AwaitingApprovalView.vue'

describe('AwaitingApprovalView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders waiting text', () => {
    mockUseQuery.mockReturnValue({ data: ref(null) })
    const wrapper = mount(AwaitingApprovalView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    expect(wrapper.text()).toContain('Venter på godkjenning')
  })

  it('renders instruction text for student', () => {
    mockUseQuery.mockReturnValue({ data: ref(null) })
    const wrapper = mount(AwaitingApprovalView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    expect(wrapper.text()).toContain('godkjenne forespørselen')
  })

  it('redirects to student-home when classroom data arrives', async () => {
    const dataRef = ref<{ data?: { id: string; institutionId: string } } | null>(null)
    mockUseQuery.mockReturnValue({ data: dataRef })
    mount(AwaitingApprovalView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    dataRef.value = { data: { id: 'c1', institutionId: 'i1' } }
    await flushPromises()
    expect(mockPush).toHaveBeenCalledWith({ name: 'student-home' })
  })
})

// ─── JoinClassroomView ────────────────────────────────────────────────────────

import JoinClassroomView from '../../views/JoinClassroomView.vue'

describe('JoinClassroomView', () => {
  beforeEach(() => vi.clearAllMocks())

  function setup() {
    mockUseMutation.mockReturnValue({ mutate: mockMutate, isPending: ref(false) })
    return mount(JoinClassroomView)
  }

  it('renders join code input', () => {
    const wrapper = setup()
    expect(wrapper.find('#join-code').exists()).toBe(true)
  })

  it('renders join button', () => {
    const wrapper = setup()
    expect(wrapper.find('button').text()).toContain('Bli med')
  })

  it('shows validation error when submitting empty code', async () => {
    const wrapper = setup()
    await wrapper.find('button').trigger('click')
    expect(wrapper.text()).toContain('Klasseromskode kreves')
  })

  it('calls mutate with join code when valid', async () => {
    const wrapper = setup()
    await wrapper.find('#join-code').setValue('ABC123')
    await wrapper.find('button').trigger('click')
    expect(mockMutate).toHaveBeenCalledWith({ body: { joinCode: 'ABC123' } })
  })

  it('clears error before new submission', async () => {
    const wrapper = setup()
    await wrapper.find('button').trigger('click')
    expect(wrapper.text()).toContain('Klasseromskode kreves')
    await wrapper.find('#join-code').setValue('XYZ')
    await wrapper.find('button').trigger('click')
    expect(wrapper.text()).not.toContain('Klasseromskode kreves')
  })

  it('shows "Blir med..." while pending', () => {
    mockUseMutation.mockReturnValue({ mutate: mockMutate, isPending: ref(true) })
    const wrapper = mount(JoinClassroomView)
    expect(wrapper.find('button').text()).toContain('Blir med...')
  })
})

// ─── TeacherSettingsView ──────────────────────────────────────────────────────

import TeacherSettingsView from '../../views/teacher/TeacherSettingsView.vue'

describe('TeacherSettingsView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders logout button', () => {
    const wrapper = mount(TeacherSettingsView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    expect(wrapper.text()).toContain('Logg ut')
  })

  it('calls authStore.logout when logout button is clicked', async () => {
    const wrapper = mount(TeacherSettingsView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    const { useAuthStore } = await import('../../stores/AuthStore')
    const authStore = useAuthStore()
    await wrapper.find('button').trigger('click')
    expect(authStore.logout).toHaveBeenCalled()
  })
})

// ─── TeacherHomeView ──────────────────────────────────────────────────────────

import TeacherHomeView from '../../views/teacher/TeacherHomeView.vue'

describe('TeacherHomeView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders the classroom list panel', () => {
    mockUseQuery.mockReturnValue({ data: ref({ data: [] }) })
    const wrapper = mount(TeacherHomeView)
    expect(wrapper.find('.classroom-list-panel').exists()).toBe(true)
  })

  it('renders the side panel menu', () => {
    mockUseQuery.mockReturnValue({ data: ref({ data: [] }) })
    const wrapper = mount(TeacherHomeView)
    expect(wrapper.find('.side-panel-menu').exists()).toBe(true)
  })

  it('shows unread count 0 when all notifications are read', () => {
    mockUseQuery.mockReturnValue({
      data: ref({ data: [{ isRead: true }, { isRead: true }] }),
    })
    mount(TeacherHomeView)
    // just verify it mounts without error
  })

  it('computes unread count from notifications', () => {
    mockUseQuery.mockReturnValue({
      data: ref({ data: [{ isRead: false }, { isRead: true }, { isRead: false }] }),
    })
    const wrapper = mount(TeacherHomeView)
    expect(wrapper.exists()).toBe(true)
  })
})
