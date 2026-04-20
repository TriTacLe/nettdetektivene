import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/start' },
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
      path: '/join-classroom',
      name: 'join-classroom',
      component: () => import('../views/JoinClassroomView.vue'),
    },
    {
      path: '/task-info',
      name: 'task-info',
      component: () => import('../views/TaskInfoView.vue'),
    },
  ],
})

export default router
