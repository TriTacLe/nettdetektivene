import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ClassroomSettingsView from '../../views/teacher/ClassroomSettingsView.vue'

vi.mock('@/components/SceneComponent.vue', () => ({
  default: { name: 'SceneComponent', template: '<div><slot /></div>' },
}))

vi.mock('@/composables/UseFocusTrap', () => ({
  useFocusTrap: () => ({ onKeydown: vi.fn() }),
}))

const mockPush = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({ push: mockPush }),
    useRoute: () => ({ params: { id: 'classroom-1' }, query: {} }),
  }
})

const mockGetInstructors = vi.fn().mockResolvedValue([{ id: 'u1' }, { id: 'u2' }])
const mockDeleteClassroom = vi.fn().mockResolvedValue(undefined)
const mockLeaveClassroom = vi.fn().mockResolvedValue(undefined)
const mockAddInstructor = vi.fn().mockResolvedValue(undefined)

vi.mock('@/services/ClassroomService.ts', () => ({
  default: {
    getInstructors: (...args: unknown[]) => mockGetInstructors(...args),
    deleteClassroom: (...args: unknown[]) => mockDeleteClassroom(...args),
    leaveClassroom: (...args: unknown[]) => mockLeaveClassroom(...args),
    addInstructor: (...args: unknown[]) => mockAddInstructor(...args),
  },
}))

function setup() {
  return mount(ClassroomSettingsView, {
    attachTo: document.body,
  })
}

describe('ClassroomSettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })

  it('renders all menu items', async () => {
    const wrapper = setup()
    await flushPromises()
    expect(wrapper.text()).toContain('Rediger Klasserom')
    expect(wrapper.text()).toContain('Slett Klasserom')
    expect(wrapper.text()).toContain('Inviter Lærer')
    expect(wrapper.text()).toContain('Forlat Klasserom')
  })

  it('fetches instructor count on mount', async () => {
    setup()
    await flushPromises()
    expect(mockGetInstructors).toHaveBeenCalledWith('classroom-1')
  })

  it('does not show delete dialog initially', async () => {
    const wrapper = setup()
    await flushPromises()
    expect(wrapper.find('#delete-dialog').exists()).toBe(false)
  })

  it('opens delete dialog when "Slett Klasserom" is clicked', async () => {
    const wrapper = setup()
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const deleteBtn = buttons.find((b) => b.text().includes('Slett Klasserom'))
    await deleteBtn!.trigger('click')
    expect(document.querySelector('#delete-dialog')).not.toBeNull()
  })

  it('closes delete dialog when cancel is clicked', async () => {
    const wrapper = setup()
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const deleteBtn = buttons.find((b) => b.text().includes('Slett Klasserom'))
    await deleteBtn!.trigger('click')
    const cancelBtn = document.querySelector<HTMLButtonElement>('[data-action="cancel"]')
    cancelBtn?.click()
    await flushPromises()
    expect(document.querySelector('#delete-dialog')).toBeNull()
  })

  it('calls deleteClassroom and redirects on confirm delete', async () => {
    const wrapper = setup()
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const deleteBtn = buttons.find((b) => b.text().includes('Slett Klasserom'))
    await deleteBtn!.trigger('click')

    const dialogBtns = Array.from(
      document.querySelectorAll<HTMLButtonElement>('#delete-dialog button'),
    )
    const confirmBtn = dialogBtns.find((b) => b.textContent?.trim() === 'Slett')
    confirmBtn?.click()
    await flushPromises()

    expect(mockDeleteClassroom).toHaveBeenCalledWith('classroom-1')
    expect(mockPush).toHaveBeenCalledWith({ name: 'teacher-home' })
  })

  it('opens invite dialog when "Inviter Lærer" is clicked', async () => {
    const wrapper = setup()
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const inviteBtn = buttons.find((b) => b.text().includes('Inviter Lærer'))
    await inviteBtn!.trigger('click')
    expect(document.querySelector('#invite-dialog')).not.toBeNull()
  })

  it('shows validation error in invite dialog when email is empty', async () => {
    const wrapper = setup()
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const inviteBtn = buttons.find((b) => b.text().includes('Inviter Lærer'))
    await inviteBtn!.trigger('click')
    const submitBtn = document.querySelector<HTMLButtonElement>(
      '#invite-dialog button[type="submit"]',
    )
    submitBtn?.click()
    await flushPromises()
    expect(document.body.textContent).toContain('E-postadresse er påkrevd')
  })

  it('opens leave dialog when "Forlat Klasserom" is clicked', async () => {
    const wrapper = setup()
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const leaveBtn = buttons.find((b) => b.text().includes('Forlat Klasserom'))
    await leaveBtn!.trigger('click')
    expect(document.querySelector('#leave-dialog')).not.toBeNull()
  })

  it('calls leaveClassroom and redirects on confirm leave', async () => {
    const wrapper = setup()
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const leaveBtn = buttons.find((b) => b.text().includes('Forlat Klasserom'))
    await leaveBtn!.trigger('click')

    const dialogBtns = Array.from(
      document.querySelectorAll<HTMLButtonElement>('#leave-dialog button'),
    )
    const confirmBtn = dialogBtns.find((b) => b.textContent?.trim() === 'Forlat')
    confirmBtn?.click()
    await flushPromises()

    expect(mockLeaveClassroom).toHaveBeenCalledWith('classroom-1')
    expect(mockPush).toHaveBeenCalledWith({ name: 'teacher-home' })
  })

  it('shows delete error when deleteClassroom fails', async () => {
    mockDeleteClassroom.mockRejectedValueOnce(new Error('Sletting feilet'))
    const wrapper = setup()
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const deleteBtn = buttons.find((b) => b.text().includes('Slett Klasserom'))
    await deleteBtn!.trigger('click')

    const dialogBtns = Array.from(
      document.querySelectorAll<HTMLButtonElement>('#delete-dialog button'),
    )
    const confirmBtn = dialogBtns.find((b) => b.textContent?.trim() === 'Slett')
    confirmBtn?.click()
    await flushPromises()

    expect(document.body.textContent).toContain('Sletting feilet')
  })
})
