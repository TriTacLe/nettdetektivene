import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/AuthStore'
import { useStudentStore } from '@/stores/StudentStore'

/**
 * Routes a freshly-authenticated student to the right next screen:
 * set-display-name if they still carry the default email-prefix name,
 * student-home if they belong to a classroom, awaiting-approval while
 * a join request is pending, otherwise join-classroom. Shared by the
 * student login flow and the main menu Continue action.
 */
export function useStudentPostLoginRoute() {
  const router = useRouter()
  const authStore = useAuthStore()
  const studentStore = useStudentStore()

  async function go(): Promise<void> {
    const emailPrefix = authStore.user?.email?.split('@')[0]
    if (authStore.user?.displayName === emailPrefix) {
      router.push({ name: 'set-display-name' })
      return
    }

    const headers = { Authorization: `Bearer ${authStore.token}` }
    const res = await fetch(`${import.meta.env.VITE_API_URL}/classrooms/me`, { headers })

    if (res.ok) {
      const body = await res.json()
      const classroomId = body.data?.id
      const institutionId = body.data?.institutionId
      if (classroomId && institutionId) {
        studentStore.setClassroom(classroomId, institutionId)
      }
      router.push({ name: 'student-home' })
      return
    }

    const statusRes = await fetch(
      `${import.meta.env.VITE_API_URL}/classrooms/me/status`,
      { headers },
    )
    if (statusRes.ok) {
      router.push({ name: 'awaiting-approval' })
    } else {
      router.push({ name: 'join-classroom' })
    }
  }

  return { go }
}
