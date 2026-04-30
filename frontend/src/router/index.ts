import { createRouter, createWebHistory } from 'vue-router'
import { useStopStore } from '@/stores/StopStore'
import { useAuthStore } from '@/stores/AuthStore'
import './types'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: { name: 'start' } },
    { path: '/start', name: 'start', component: () => import('../views/StartView.vue') },
    {
      path: '/student-login',
      name: 'student-login',
      component: () => import('../views/StudentLoginView.vue'),
    },
    {
      path: '/student-home',
      name: 'student-home',
      component: () => import('../views/StudentHomeView.vue'),
    },
    {
      path: '/teacher-login',
      name: 'teacher-login',
      component: () => import('../views/TeacherLoginView.vue'),
    },
    {
      path: '/game-map',
      name: 'game-map',
      component: () => import('../views/GameMapView.vue'),
    },
    {
      path: '/stops/nyhetskvartalet/:taskId',
      name: 'nyhetskvartalet-task',
      component: () => import('../views/NyhetskvartaletTaskView.vue'),
      meta: { music: 'dark' },
    },
    {
      path: '/stops/fotografen/:taskId',
      name: 'fotografen-task',
      component: () => import('../views/FotografenTaskView.vue'),
      meta: { music: 'dark' },
    },
    {
      path: '/stops/:stopId/:taskId',
      name: 'stop-task',
      component: () => import('../views/TaskView.vue'),
    },
    {
      path: '/stops/:stopId',
      name: 'stop',
      component: () => import('../views/StopView.vue'),
      meta: { music: 'dark' },
    },
    {
      path: '/join-classroom',
      name: 'join-classroom',
      component: () => import('../views/JoinClassroomView.vue'),
    },
    {
      path: '/set-display-name',
      name: 'set-display-name',
      component: () => import('../views/SetDisplayNameView.vue'),
    },
    {
      path: '/student-profile',
      name: 'student-profile',
      component: () => import('../views/StudentProfileView.vue'),
    },
    {
      path: '/student-profile/avatar',
      name: 'edit-avatar',
      component: () => import('../views/EditAvatarView.vue'),
    },
    {
      path: '/student/classrooms/:classroomId/mystery',
      name: 'student-weekly-mystery',
      component: () => import('../views/WeeklyMysteryChoiceView.vue'),
      meta: { music: 'mystery' },
    },
    {
      path: '/student/classrooms/:classroomId/mystery/play',
      name: 'student-weekly-mystery-play',
      component: () => import('../views/WeeklyMysteryPlayView.vue'),
      meta: { music: 'mystery' },
    },
    {
      path: '/student/classrooms/:classroomId/mystery/play/:submissionId',
      name: 'student-weekly-mystery-detail',
      component: () => import('../views/WeeklyMysteryDetailView.vue'),
      meta: { music: 'mystery' },
    },
    {
      path: '/student/classrooms/:classroomId/mystery/submit',
      name: 'student-weekly-mystery-submit',
      component: () => import('../views/WeeklyMysteryStudentView.vue'),
      meta: { music: 'mystery' },
    },
    {
      path: '/teacher-home',
      name: 'teacher-home',
      component: () => import('../views/teacher/TeacherHomeView.vue'),
    },
    {
      path: '/classroom/:id/test-game',
      name: 'classroom-test-game',
      component: () => import('../views/teacher/TeacherTestGameView.vue'),
    },
    {
      path: '/teacher-leaderboard',
      name: 'teacher-leaderboard',
      component: () => import('../views/teacher/TeacherLeaderboardView.vue'),
    },
    {
      path: '/teacher-leaderboard/:classroomId',
      name: 'teacher-classroom-leaderboard',
      component: () => import('../views/teacher/TeacherClassroomLeaderboardView.vue'),
    },
    {
      path: '/teacher-settings',
      name: 'settings',
      component: () => import('../views/teacher/TeacherSettingsView.vue'),
    },
    {
      path: '/classroom/new',
      name: 'create-classroom',
      component: () => import('../views/teacher/CreateClassroomView.vue'),
    },
    {
      path: '/classroom/:id',
      name: 'classroom-detail',
      component: () => import('../views/teacher/ClassroomDetailView.vue'),
      // beforeEnter: requireTeacher,
    },
    {
      path: '/classroom/:id/tasks',
      name: 'classroom-tasks',
      component: () => import('../views/teacher/ClassroomDetailView.vue'),
    },
    {
      path: '/classroom/:id/tasks/new',
      name: 'classroom-task-create',
      component: () => import('../views/teacher/CreateTaskView.vue'),
    },
    {
      path: '/classroom/:id/tasks/:taskId/edit',
      name: 'classroom-task-edit',
      component: () => import('../views/teacher/EditClassroomTaskView.vue'),
    },
    {
      path: '/classroom/:id/students',
      name: 'classroom-students',
      component: () => import('../views/teacher/ClassroomStudentsView.vue'),
    },
    {
      path: '/classroom/:id/settings',
      name: 'classroom-settings',
      component: () => import('../views/teacher/ClassroomSettingsView.vue'),
    },
    {
      path: '/classroom/:id/edit',
      name: 'edit-classroom',
      component: () => import('../views/teacher/CreateClassroomView.vue'),
    },
    {
      path: '/classroom/:id/progress/:userId',
      name: 'classroom-student-progress',
      component: () => import('../views/teacher/ClassroomStudentProgressView.vue'),
    },
    {
      path: '/notebook',
      name: 'notebook',
      component: () => import('../views/NotebookView.vue'),
    },
    {
      path: '/student-leaderboard',
      name: 'student-leaderboard',
      component: () => import('../views/StudentLeaderboardView.vue'),
    },
    {
      path: '/awaiting-approval',
      name: 'awaiting-approval',
      component: () => import('../views/AwaitingApprovalView.vue'),
    },
    {
      path: '/ending',
      name: 'ending',
      component: () => import('../views/EndingView.vue'),
      meta: { music: 'datasenteret' },
    },
    {
      path: '/task-finished',
      name: 'task-finished',
      component: () => import('../views/TaskFinishedView.vue'),
      meta: { music: 'dark' },
    },
    {
      path: '/stop-finished',
      name: 'stop-finished',
      component: () => import('../views/StopFinishedView.vue'),
      meta: { music: 'dark' },
    },
    {
      path: '/task-failed',
      name: 'task-failed',
      component: () => import('../views/TaskFailedView.vue'),
      meta: { music: 'dark' },
    },
    {
      path: '/teacher-notifications',
      name: 'teacher-notifications',
      component: () => import('../views/teacher/NotificationView.vue'),
    },
    {
      path: '/achievements',
      name: 'achievements',
      component: () => import('../views/AchievementsView.vue'),
    },
    {
      path: '/tasks',
      name: 'tasks',
      component: () => import('../views/TasksView.vue'),
      meta: { music: 'dark' },
    },
  ],
})

const PUBLIC_ROUTES = new Set([
  'start', 'student-login', 'teacher-login'
])

const AUTH_ROUTES = new Set([
  'join-classroom', 'set-display-name', 'awaiting-approval',
])

const TEACHER_ROUTES = new Set([
  'teacher-home', 'classroom-test-game', 'teacher-leaderboard',
  'teacher-classroom-leaderboard', 'settings',
  'create-classroom', 'classroom-detail',
  'classroom-task-create', 'classroom-task-edit', 'classroom-students',
  'classroom-settings', 'edit-classroom', 'classroom-student-progress',
  'teacher-notifications',
])

const STUDENT_ROUTES = new Set([
  'student-home', 'game-map', 'nyhetskvartalet-task',
  'fotografen-task', 'stop-task', 'stop',
  'student-profile', 'edit-avatar', 'notebook',
  'student-leaderboard', 'ending', 'task-finished',
  'stop-finished', 'task-failed', 'tasks',
  'achievements',
  'student-weekly-mystery', 'student-weekly-mystery-play', 'student-weekly-mystery-detail', 'student-weekly-mystery-submit',
])

// Routes that belong to the test-mode flow. Leaving any of these while
// testModeClassroomId is set means the teacher exited test mode, so we
// drop the flag + the local "completed" marks loadTasksTestMode left.
function isTestModeRoute(path: string, name: unknown): boolean {
  if (path.startsWith('/stops/')) return true
  if (/^\/classroom\/[^/]+\/test-game$/.test(path)) return true
  if (typeof name === 'string') {
    return ['task-finished', 'task-failed', 'stop-finished', 'ending'].includes(name)
  }
  return false
}

router.beforeEach((to) => {
  const stop = useStopStore()
  if (stop.testModeClassroomId && !isTestModeRoute(to.path, to.name)) {
    stop.exitTestMode()
  }

  const auth = useAuthStore()
  const name = to.name = to.name as string

  if (PUBLIC_ROUTES.has(name)) return true
  if (!auth.isAuthenticated) return { name: 'start' }
  if (TEACHER_ROUTES.has(name) && !auth.isTeacher) return { name: 'start' }
  if (STUDENT_ROUTES.has(name) && !auth.isStudent && !isTestModeRoute(to.path, to.name)) return { name: 'start' }
  if (AUTH_ROUTES.has(name) && !auth.isStudent) return true

  return true
})

export default router
