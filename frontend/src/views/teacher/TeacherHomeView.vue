<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useQueries } from '@tanstack/vue-query'
import {
  Users,
  ClipboardList,
  BarChart3,
  Plus,
  BookOpen,
  Trophy,
  Settings,
  Bell,
} from 'lucide-vue-next'
import SceneComponent from "@/components/SceneComponent.vue"
import {
  getMyNotificationsOptions,
  getAllClassroomsOptions,
  getApprovedStudentsOptions,
  getClassroomTasksOptions,
} from '@/generated-client/@tanstack/vue-query.gen'

const router = useRouter()

const { data: classroomsData } = useQuery(getAllClassroomsOptions())
const { data: notificationsData } = useQuery({ ...getMyNotificationsOptions(), refetchInterval: 5000 })

const classrooms = computed(() => classroomsData.value?.data ?? [])

const studentQueries = useQueries({
  queries: computed(() =>
    classrooms.value.map((c) =>
      getApprovedStudentsOptions({ path: { classroomId: c.id! } })
    )
  ),
})

const taskQueries = useQueries({
  queries: computed(() =>
    classrooms.value.map((c) =>
      getClassroomTasksOptions({ path: { classroomId: c.id! } })
    )
  ),
})

function studentCount(index: number) {
  return studentQueries.value[index]?.data?.data?.length ?? 0
}

function taskCount(index: number) {
  return taskQueries.value[index]?.data?.data?.length ?? 0
}

function classroomNotifications(classroomId: string) {
  return (notificationsData.value?.data ?? []).filter(
    (n) => !n.isRead && n.classroomId === classroomId
  ).length
}

const footerItems = [
  { label: 'Oppgaver', icon: BookOpen, route: 'tasks' },
  { label: 'Ledertavler', icon: Trophy, route: 'teacher-leaderboard' },
  { label: 'Varsler', icon: Bell, route: 'teacher-notifications' },
  { label: 'Innstillinger', icon: Settings, route: 'settings' },
]
</script>

<template>
  <SceneComponent
    title="Laererportal"
    background="/StartBackground.jpg"
    dim-overlay>

    <div class="home-content">
      <!-- Classroom cards -->
      <div class="card-list">
        <article
          v-for="(classroom, i) in classrooms"
          :key="classroom.id"
          class="classroom-card"
          :style="{ animationDelay: `${i * 0.07}s` }"
        >
          <div class="card-header">
            <h2 class="card-title">{{ classroom.title }}</h2>
            <span
              v-if="classroomNotifications(classroom.id!)"
              class="card-badge"
              :aria-label="`${classroomNotifications(classroom.id!)} uleste varsler`"
            >
              {{ classroomNotifications(classroom.id!) }}
            </span>
          </div>

          <div class="card-stats">
            <span class="stat">
              <Users :size="14" aria-hidden="true" />
              {{ studentCount(i) }} elever
            </span>
            <span class="stat">
              <ClipboardList :size="14" aria-hidden="true" />
              {{ taskCount(i) }} oppgaver
            </span>
            <span class="stat stat-code">
              {{ classroom.joinCode }}
            </span>
          </div>

          <div class="card-actions">
            <button
              class="action-btn"
              @click="router.push({ name: 'classroom-detail', params: { id: classroom.id } })"
            >
              Apne
            </button>
            <button
              class="action-btn"
              @click="router.push({ name: 'classroom-tasks', params: { id: classroom.id } })"
            >
              Oppgaver
            </button>
            <button
              class="action-btn"
              @click="router.push({ name: 'classroom-students', params: { id: classroom.id } })"
            >
              <BarChart3 :size="14" aria-hidden="true" />
              Elever
            </button>
          </div>
        </article>

        <!-- Empty state / create -->
        <button
          class="create-card"
          :style="{ animationDelay: `${classrooms.length * 0.07}s` }"
          @click="router.push({ name: 'create-classroom' })"
          aria-label="Opprett nytt klasserom"
        >
          <Plus :size="24" aria-hidden="true" />
          <span>Nytt klasserom</span>
        </button>
      </div>

      <!-- Footer nav -->
      <footer class="footer-nav" aria-label="Ekstra navigasjon">
        <button
          v-for="item in footerItems"
          :key="item.route"
          class="footer-btn"
          :aria-label="item.label"
          @click="router.push({ name: item.route })"
        >
          <component :is="item.icon" :size="18" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </button>
      </footer>
    </div>
  </SceneComponent>
</template>

<style scoped>
.home-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 1.5rem 1.5rem;
  max-width: 32rem;
  width: 100%;
  margin: 0 auto;
}

.card-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* ---- Classroom card ---- */
.classroom-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 1rem;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 4px 12px rgb(0 0 0 / 0.12);
  animation: slideUp 0.5s ease-out both;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.625rem;
}

.card-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #111827;
  margin: 0;
  line-height: 1.3;
}

.card-badge {
  background: #ef4444;
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: 9999px;
  min-width: 1.375rem;
  height: 1.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.375rem;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #4b5563;
}

.stat-code {
  font-family: monospace;
  font-weight: 700;
  color: #6b7280;
  letter-spacing: 0.05em;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 0.5rem;
  background: white;
  font-size: 0.875rem;
  font-weight: 600;
  color: #1f2937;
  cursor: pointer;
  transition: background 0.15s, box-shadow 0.15s;
}

.action-btn:hover {
  background: #f9fafb;
  box-shadow: 0 2px 6px rgb(0 0 0 / 0.08);
}

.action-btn:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

/* ---- Create card ---- */
.create-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.25rem;
  border: 2px dashed rgba(255, 255, 255, 0.35);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.9375rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  animation: slideUp 0.5s ease-out both;
}

.create-card:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.5);
}

.create-card:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

/* ---- Footer ---- */
.footer-nav {
  display: flex;
  justify-content: space-around;
  padding: 0.75rem 0;
  margin-top: 1.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}

.footer-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem;
  border: none;
  background: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.6875rem;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.15s;
}

.footer-btn:hover {
  color: white;
}

.footer-btn:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
