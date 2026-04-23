import { createRouter, createWebHistory } from 'vue-router'
import { requireTeacher } from "@/router/guards.ts";

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
      path: '/stops/nyhetskvartalet',
      name: 'nyhetskvartalet',
      component: () => import('../views/NyhetskvartaletView.vue'),
    },
    {
      path: '/stops/nyhetskvartalet/:taskId',
      name: 'nyhetskvartalet-task',
      component: () => import('../views/NyhetskvartaletTaskView.vue'),
    },
    {
      path: '/stops/fotografen',
      name: 'fotografen',
      component: () => import('../views/FotografenView.vue'),
    },
    {
      path: '/stops/fotografen/:taskId',
      name: 'fotografen-task',
      component: () => import('../views/FotografenTaskView.vue'),
    },
    {
      path: '/stops/:stopId',
      name: 'stop',
      component: () => import('../views/StopView.vue'),
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
      path: '/teacher-home',
      name: 'teacher-home',
      component: () => import('../views/teacher/TeacherHomeView.vue'),
      // beforeEnter: requireTeacher,
    },
    {
      path: '/classroom-overview',
      name: 'classroom-overview',
      component: () => import('../views/teacher/ClassroomOverviewView.vue'),
      // beforeEnter: requireTeacher,
    },
    {
      path: '/classroom/new',
      name: 'create-classroom',
      component: () => import('../views/teacher/CreateClassroomView.vue'),
      // beforeEnter: requireTeacher,
    },
    {
      path: '/classroom/:id',
      name: 'classroom-detail',
      component: () => import('../views/teacher/ClassroomDetailView.vue'),
      // beforeEnter: requireTeacher,
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
      component: () => import('../views/teacher/CreateClassroomView.vue')
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
      path: '/introduction',
      name: 'introduction',
      component: () => import('../views/Introduction.vue'),
    },
    {
      path: '/task-finished',
      name: 'task-finished',
      component: () => import('../views/TaskFinishedView.vue'),
    },
    {
      path: '/stop-finished',
      name: 'stop-finished',
      component: () => import('../views/StopFinishedView.vue'),
    },
    {
      path: '/task-failed',
      name: 'task-failed',
      component: () => import('../views/TaskFailedView.vue'),
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
    },
  ],
})
export default router
