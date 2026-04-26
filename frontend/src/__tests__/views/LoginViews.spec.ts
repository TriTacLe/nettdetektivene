import { describe, it, expect, vi, beforeEach} from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createTestingPinia } from '@pinia/testing';
import { createRouter, createWebHashHistory } from "vue-router";

import StudentLoginView from '../../views/StudentLoginView.vue'
import TeacherLoginView from '../../views/TeacherLoginView.vue'
import { useAuthStore } from '../../stores/AuthStore'

vi.mock('@/components/SceneComponent.vue', () => ({
  default: {
    name: 'SceneComponent',
    template: '<div><slot /></div>'
  }
}))

const mockPush = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return { ...actual, useRouter: () => ({ push: mockPush })}
})

const router = createRouter({
  history: createWebHashHistory(),
  routes: [{ path: '/', component: { template: '<div />' } }],
})

function mountView(component: object) {
  return mount(component, {
    global: {
      plugins: [createTestingPinia({ createSpy: vi.fn }), router],
    },
  })
}

async function fillForm(wrapper: ReturnType<typeof mount>, email: string, password: string) {
  await wrapper.find('input[type="email"]').setValue(email)
  await wrapper.find('input[type="password"]').setValue(password)
}

async function submit(wrapper: ReturnType<typeof mount>) {
  await wrapper.find('button').trigger('click')
  await flushPromises()
}

// StudentLoginView
describe('StudentLoginView', () => {
  let wrapper: ReturnType<typeof mount>
  let authStore: ReturnType<typeof useAuthStore>

  beforeEach(() => {
    vi.clearAllMocks()
    wrapper = mountView(StudentLoginView)
    authStore = useAuthStore()
  })

  it('shows email and password fields', () => {
    expect(wrapper.find('input[type="email"]').exists()).toBe(true)
    expect(wrapper.find('input[type="password"]').exists()).toBe(true)
  })

  it('shows error if empty submit', async () => {
    await wrapper.find('input[type="email"]').setValue('')
    await wrapper.find('input[type="password"]').setValue('')
    await submit(wrapper)
    expect(wrapper.text()).toContain('Epost kreves')
    expect(wrapper.text()).toContain('Passord kreves')
  })

  it('doesnt call on feideLogin if validation error', async () => {
    await wrapper.find('input[type="email"]').setValue('')
    await wrapper.find('input[type="password"]').setValue('')
    await submit(wrapper)
    expect(authStore.feideLogin).not.toHaveBeenCalled()
  })

  it('calls on feideLogin if valid input and affiliation student', async () => {
    vi.mocked(authStore.feideLogin).mockResolvedValueOnce(undefined)
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: false,
    } as Response)
    await fillForm(wrapper, 'kari@skole.no', 'passord123')
    await submit(wrapper)
    expect(authStore.feideLogin).toHaveBeenCalledWith({
      email: 'kari@skole.no',
      affiliation: 'student',
      password: 'passord123',
    })
  })

  it('navigates to set-display-name when displayName equals email prefix', async () => {
    vi.mocked(authStore.feideLogin).mockResolvedValueOnce(undefined)
    Object.defineProperty(authStore, 'user', {
      get: () => ({ email: 'kari@skole.no', displayName: 'kari' }),
      configurable: true
    })
    await fillForm(wrapper, 'kari@skole.no', 'passord123')
    await submit(wrapper)
    expect(mockPush).toHaveBeenCalledWith({ name: 'set-display-name' })
  })

  it('navigates to join-classroom when student has no classroom', async () => {
    vi.mocked(authStore.feideLogin).mockResolvedValueOnce(undefined)
    Object.defineProperty(authStore, 'user', {
      get: () => ({ email: 'kari@skole.no', displayName: 'Kari Nordmann' }),
      configurable: true
    })
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: false,
    } as Response)
    await fillForm(wrapper, 'kari@skole.no', 'passord123')
    await submit(wrapper)
    expect(mockPush).toHaveBeenCalledWith({ name: 'join-classroom' })
  })

  it('shows error if unsuccessful login', async () => {
    vi.mocked(authStore.feideLogin).mockRejectedValueOnce(new Error('Ugyldig epost eller passord'))
    await fillForm(wrapper, 'kari@skole.no', 'passord123')
    await submit(wrapper)
    expect(wrapper.text()).toContain('Ugyldig epost eller passord')
  })
})

// TeacherLoginView

describe('TeacherLoginView', () => {
  let wrapper: ReturnType<typeof mount>
  let authStore: ReturnType<typeof useAuthStore>

  beforeEach(() => {
    vi.clearAllMocks()
    wrapper = mountView(TeacherLoginView)
    authStore = useAuthStore()
  })

  it('calls login with email and password', async () => {
    vi.mocked(authStore.login).mockResolvedValueOnce(undefined)
    await fillForm(wrapper, 'lars@skole.no', 'passord123')
    await submit(wrapper)
    expect(authStore.login).toHaveBeenCalledWith({
      email: 'lars@skole.no',
      password: 'passord123'
    })
  })

  it('navigates to teacher-home after successful login', async() => {
    vi.mocked(authStore.login).mockResolvedValueOnce(undefined)
    await fillForm(wrapper, 'lars@skole.no', 'passord123')
    await submit(wrapper)
    expect(mockPush).toHaveBeenCalledWith({ name: 'teacher-home' })
  })

  it('shows error if unsuccessful login', async () => {
    vi.mocked(authStore.login).mockRejectedValueOnce(new Error('Ugyldig epost eller passord'))
    await fillForm(wrapper, 'lars@skole.no', 'passord123')
    await submit(wrapper)
    expect(wrapper.text()).toContain('Ugyldig epost eller passord')
  })

})





















