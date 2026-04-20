import { describe, it, expect, vi, beforeEach} from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createTestingPinia } from '@pinia/testing';
import { createRouter, createWebHashHistory } from "vue-router";

import StudentLoginView from "@/views/StudentLoginView.vue";
import TeacherLoginView from "@/views/TeacherLoginView.vue";
import { useAuthStore } from "@/stores/AuthStore";

vi.mock('@/components/SceneComponent.vue', () => ({
  default: {
    name: 'SceneComponent',
    template: '<div><slot /></div>'
  }
}))

const mockPush = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return { ...actual, useRouter: () => ({ push.mockPush })}
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
  await wrapper.find('input[type="email').setValue(email)
  await wrapper.find('input[type="password').setValue(password)
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
    vi.clearAllMocks
  })
}
