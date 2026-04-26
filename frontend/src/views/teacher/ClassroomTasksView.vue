<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery } from '@tanstack/vue-query'
import { getClassroomOptions } from '@/generated-client/@tanstack/vue-query.gen'
import ClassroomService from '@/services/ClassroomService'
import type { ClassroomTaskResponseDTO } from '@/types'

const route = useRoute()
const router = useRouter()
const classroomId = route.params.id as string

const { data: classroomData } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroom = computed(() => classroomData.value?.data ?? null)

const tasksQuery = useQuery({
  queryKey: ['teacher-classroom-tasks', classroomId],
  queryFn: () => ClassroomService.getClassroomTasks(classroomId, false, false),
})

const tasks = computed<ClassroomTaskResponseDTO[]>(() => tasksQuery.data.value ?? [])

function openCreateTask() {
  router.push({ name: 'classroom-task-create', params: { id: classroomId } })
}
</script>

<template>
  <SceneComponent
    :title="classroom?.title ? `Oppgaver: ${classroom.title}` : 'Oppgaver'"
    :previous="`/classroom/${classroomId}`"
    background="/StartBackground.jpg"
  >
    <div class="teacher-tasks-page">
      <section class="teacher-panel teacher-panel--hero">
        <div>
          <p class="teacher-eyebrow">Klasseromsoppgaver</p>
          <h2 class="teacher-title">Lag og administrer oppgaver for dette klasserommet</h2>
          <p class="teacher-copy">
            Opprett en ny oppgave for Nyhetskvartalet eller Fotografen, og legg den direkte til i klassens løype.
          </p>
        </div>
        <button class="teacher-primary-btn" @click="openCreateTask">
          Lag oppgave
        </button>
      </section>

      <div v-if="tasksQuery.isLoading.value" class="teacher-state">Laster oppgaver...</div>
      <div v-else-if="tasksQuery.error.value" class="teacher-state teacher-state--error">
        Kunne ikke hente oppgavene.
      </div>
      <section v-else-if="tasks.length" class="teacher-task-grid">
        <article v-for="item in tasks" :key="item.taskId" class="teacher-task-card">
          <div class="teacher-task-meta">
            <span class="teacher-pill">{{ item.task.taskType }}</span>
            <span class="teacher-pill teacher-pill--muted">Sortering {{ item.sortOrder }}</span>
            <span
              class="teacher-pill"
              :class="item.isActive ? 'teacher-pill--active' : 'teacher-pill--inactive'"
            >
              {{ item.isActive ? 'Aktiv' : 'Inaktiv' }}
            </span>
          </div>
          <h3 class="teacher-task-name">{{ item.task.title }}</h3>
          <p class="teacher-task-details">
            Stop: {{ item.task.stopLocation || 'ukjent' }} • Vanskelighetsgrad {{ item.task.difficulty }}
          </p>
        </article>
      </section>
      <div v-else class="teacher-state">
        Ingen oppgaver i dette klasserommet enda.
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.teacher-tasks-page {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  overflow-y: auto;
}

.teacher-panel {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  background: linear-gradient(135deg, rgba(255, 249, 230, 0.95), rgba(255, 237, 213, 0.92));
  border: 2px solid rgba(120, 53, 15, 0.28);
  border-radius: 1.25rem;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.18);
  padding: 1.5rem;
}

.teacher-panel--hero {
  align-items: end;
}

.teacher-eyebrow {
  margin: 0 0 0.35rem;
  color: #92400e;
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.teacher-title {
  margin: 0;
  color: #2b1606;
  font-size: clamp(1.4rem, 2vw, 2rem);
  font-weight: 900;
}

.teacher-copy {
  margin: 0.5rem 0 0;
  color: #5b3414;
  max-width: 60ch;
}

.teacher-primary-btn {
  align-self: center;
  background: #92400e;
  color: white;
  border-color: #78350f;
  white-space: nowrap;
  font-weight: 900;
  -webkit-text-stroke: 1.5px #000;
  paint-order: stroke fill;
  text-shadow:
    2px 0 #000,
    -2px 0 #000,
    0 2px #000,
    0 -2px #000,
    1px 1px #000,
    -1px -1px #000,
    1px -1px #000,
    -1px 1px #000;
}

.teacher-task-grid {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.teacher-task-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 1.1rem;
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.teacher-task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.teacher-pill {
  border-radius: 999px;
  padding: 0.22rem 0.7rem;
  background: #ffedd5;
  color: #7c2d12;
  font-size: 0.78rem;
  font-weight: 800;
}

.teacher-pill--muted {
  background: #f3f4f6;
  color: #4b5563;
}

.teacher-pill--active {
  background: #dcfce7;
  color: #166534;
}

.teacher-pill--inactive {
  background: #fee2e2;
  color: #991b1b;
}

.teacher-task-name {
  margin: 0.9rem 0 0.35rem;
  color: #111827;
  font-size: 1.1rem;
  font-weight: 800;
}

.teacher-task-details {
  margin: 0;
  color: #6b7280;
}

.teacher-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 180px;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.14);
  color: white;
  font-weight: 700;
}

.teacher-state--error {
  color: #fecaca;
}

@media (max-width: 720px) {
  .teacher-panel {
    flex-direction: column;
  }

  .teacher-primary-btn {
    width: 100%;
  }
}
</style>
