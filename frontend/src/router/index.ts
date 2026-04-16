import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/youtube', name: 'youtube', component: () => import('../views/TestPage.vue'), props: { pageName: 'YouTube' } },
    { path: '/github', name: 'github', component: () => import('../views/TestPage.vue'), props: { pageName: 'GitHub' } },
  ],
})

export default router
