import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import { createTestingPinia } from '@pinia/testing'

vi.mock('@/components/SceneComponent.vue', () => ({
  default: { name: 'SceneComponent', template: '<div><slot /></div>' },
}))

vi.mock('@/components/CutsceneDialog.vue', () => ({
  default: {
    name: 'CutsceneDialog',
    props: ['scene', 'startAtLastLine'],
    template: '<div class="cutscene-dialog"><slot /></div>',
    emits: ['done', 'choose', 'back'],
  },
}))

vi.mock('@/components/CaseFile.vue', () => ({
  default: {
    name: 'CaseFile',
    props: ['tabLabel'],
    template: '<div class="case-file"><slot name="left" /></div>',
  },
}))

vi.mock('@/components/avatar/AvatarPreview.vue', () => ({
  default: {
    name: 'AvatarPreview',
    props: ['config', 'size', 'lazyLoad'],
    template: '<div class="avatar-preview" />',
  },
}))

vi.mock('@/composables/UseToast', () => ({
  useToast: () => ({ showSuccess: vi.fn(), showError: vi.fn() }),
}))

vi.mock('@/utils/avatar', () => ({
  parseAvatarConfig: vi.fn(() => ({})),
  serializeAvatarConfig: vi.fn(() => '{}'),
}))

vi.mock('@/data/MysteryDialogs', () => ({
  mysteryDialogs: {
    'mystery-start': {
      id: 'mystery-start',
      lines: [{ speaker: 'Detektiv', text: 'Hei!' }],
    },
  },
  mysteryStartId: 'mystery-start',
}))

vi.mock('@/services/TaskService', () => ({
  fetchAllTasks: vi.fn(),
}))

vi.mock('@/generated-client/@tanstack/vue-query.gen', () => ({
  getProfileOptions: vi.fn(() => ({})),
  getMyNotificationsOptions: vi.fn(() => ({})),
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

const mockUseQuery = vi.hoisted(() => vi.fn())
vi.mock('@tanstack/vue-query', () => ({
  useQuery: mockUseQuery,
}))

import { fetchAllTasks } from '../../services/TaskService'
import type { TaskDTO } from '../../types'
import { TaskType } from '../../types'

// ── TasksView ─────────────────────────────────────────────────────────────────

import TasksView from '../../views/TasksView.vue'

describe('TasksView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders loading state initially', async () => {
    vi.mocked(fetchAllTasks).mockReturnValue(new Promise(() => {}))
    const wrapper = mount(TasksView)
    expect(wrapper.text()).toContain('Laster oppgaver')
  })

  it('renders empty state when no tasks', async () => {
    vi.mocked(fetchAllTasks).mockResolvedValue([])
    const wrapper = mount(TasksView)
    await flushPromises()
    expect(wrapper.text()).toContain('Ingen oppgaver funnet')
  })

  it('renders error state when fetch fails', async () => {
    vi.mocked(fetchAllTasks).mockRejectedValue(new Error('Nettverksfeil'))
    const wrapper = mount(TasksView)
    await flushPromises()
    expect(wrapper.text()).toContain('Kunne ikke laste oppgaver')
  })

  it('renders task list with title and type', async () => {
    const tasks: TaskDTO[] = [
      {
        id: '1',
        title: 'Nyhetsoppgave',
        taskType: TaskType.NEWS,
        stopLocation: 'nyhetskvartalet',
        difficulty: 2,
        isActive: true,
        content: {},
      },
    ]
    vi.mocked(fetchAllTasks).mockResolvedValue(tasks)
    const wrapper = mount(TasksView)
    await flushPromises()
    expect(wrapper.text()).toContain('Nyhetsoppgave')
    expect(wrapper.text()).toContain('nyhetskvartalet')
  })

  it('shows Aktiv badge for active task', async () => {
    const tasks: TaskDTO[] = [
      {
        id: '1',
        title: 'Test',
        taskType: TaskType.NEWS,
        stopLocation: 'nyhetskvartalet',
        difficulty: 1,
        isActive: true,
        content: {},
      },
    ]
    vi.mocked(fetchAllTasks).mockResolvedValue(tasks)
    const wrapper = mount(TasksView)
    await flushPromises()
    expect(wrapper.text()).toContain('Aktiv')
  })

  it('shows Inaktiv badge for inactive task', async () => {
    const tasks: TaskDTO[] = [
      {
        id: '1',
        title: 'Test',
        taskType: TaskType.NEWS,
        stopLocation: null,
        difficulty: 1,
        isActive: false,
        content: {},
      },
    ]
    vi.mocked(fetchAllTasks).mockResolvedValue(tasks)
    const wrapper = mount(TasksView)
    await flushPromises()
    expect(wrapper.text()).toContain('Inaktiv')
    expect(wrapper.text()).toContain('Ikke satt')
  })

  it('formats JSON content correctly', async () => {
    const tasks: TaskDTO[] = [
      {
        id: '1',
        title: 'Test',
        taskType: TaskType.NEWS,
        stopLocation: 'nyhetskvartalet',
        difficulty: 1,
        isActive: true,
        content: { question: 'Hva?' },
      },
    ]
    vi.mocked(fetchAllTasks).mockResolvedValue(tasks)
    const wrapper = mount(TasksView)
    await flushPromises()
    expect(wrapper.text()).toContain('question')
  })
})

// ── WeeklyMysteryChoiceView ───────────────────────────────────────────────────

import WeeklyMysteryChoiceView from '../../views/WeeklyMysteryChoiceView.vue'

describe('WeeklyMysteryChoiceView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders the cutscene dialog', () => {
    const wrapper = mount(WeeklyMysteryChoiceView)
    expect(wrapper.find('.cutscene-dialog').exists()).toBe(true)
  })

  it('navigates to student-home on done', async () => {
    const wrapper = mount(WeeklyMysteryChoiceView)
    const dialog = wrapper.findComponent({ name: 'CutsceneDialog' })
    await dialog.vm.$emit('done')
    expect(mockPush).toHaveBeenCalledWith('/student-home')
  })

  it('navigates to student-home on back', async () => {
    const wrapper = mount(WeeklyMysteryChoiceView)
    const dialog = wrapper.findComponent({ name: 'CutsceneDialog' })
    await dialog.vm.$emit('back')
    expect(mockPush).toHaveBeenCalledWith('/student-home')
  })
})

// ── StudentProfileView ────────────────────────────────────────────────────────

import StudentProfileView from '../../views/StudentProfileView.vue'

describe('StudentProfileView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders loading state', () => {
    mockUseQuery.mockReturnValue({ data: ref(null), isLoading: ref(true) })
    const wrapper = mount(StudentProfileView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    expect(wrapper.text()).toContain('Laster')
  })

  it('renders profile with displayName', () => {
    mockUseQuery.mockReturnValue({
      data: ref({
        data: {
          displayName: 'DetektivAlex',
          xpLevel: 3,
          avatarConfig: null,
          nameChangeStatus: null,
        },
      }),
      isLoading: ref(false),
    })
    const wrapper = mount(StudentProfileView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    expect(wrapper.text()).toContain('DetektivAlex')
    expect(wrapper.text()).toContain('Niva 3')
  })

  it('renders avatar preview', () => {
    mockUseQuery.mockReturnValue({
      data: ref({
        data: { displayName: 'Alex', xpLevel: 1, avatarConfig: null, nameChangeStatus: null },
      }),
      isLoading: ref(false),
    })
    const wrapper = mount(StudentProfileView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    expect(wrapper.find('.avatar-preview').exists()).toBe(true)
  })

  it('shows pending name change message', () => {
    mockUseQuery.mockReturnValue({
      data: ref({
        data: { displayName: 'Alex', xpLevel: 1, avatarConfig: null, nameChangeStatus: 'PENDING' },
      }),
      isLoading: ref(false),
    })
    const wrapper = mount(StudentProfileView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    expect(wrapper.text()).toContain('venter på godkjenning')
  })

  it('navigates to edit-avatar on button click', async () => {
    mockUseQuery.mockReturnValue({
      data: ref({
        data: { displayName: 'Alex', xpLevel: 2, avatarConfig: null, nameChangeStatus: null },
      }),
      isLoading: ref(false),
    })
    const wrapper = mount(StudentProfileView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    await wrapper.find('button').trigger('click')
    expect(mockPush).toHaveBeenCalledWith({ name: 'edit-avatar' })
  })
})

// ── StopView (not-found case) ─────────────────────────────────────────────────

import StopView from '../../views/StopView.vue'

describe('StopView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows not-found message for unknown stop', async () => {
    const wrapper = mount(StopView, {
      global: { plugins: [createTestingPinia({ createSpy: vi.fn })] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Stoppested ikke funnet')
  })
})
