<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useStopStore } from '@/stores/StopStore'
import { STOPS } from '@/data/StopData'

const route = useRoute()
const router = useRouter()
const stopStore = useStopStore()

const stopId = route.params.stopId as string
const stop = computed(() => STOPS[stopId])
const testClassroomId = stopStore.testModeClassroomId
const previousRoute = computed(() =>
  testClassroomId ? `/classroom/${testClassroomId}/test-game` : '/game-map'
)
const tasks = computed(() => stopStore.tasks[stopId] ?? [])
const isLoading = computed(() => stopStore.loading[stopId] ?? false)

const availableTasks = computed(() => tasks.value.filter(t => t.isUnlocked))
const lockedTasks = computed(() => tasks.value.filter(t => !t.isUnlocked))

onMounted(() => {
  if (testClassroomId) {
    stopStore.loadTasksTestMode(testClassroomId, stopId)
  } else {
    stopStore.clearTasks(stopId)
    stopStore.loadTasks(stopId, true)
  }
})

function goToTask(taskId: number) {
  const task = tasks.value.find(t => t.taskId === taskId)
  if (task && task.isUnlocked && stop.value) {
    router.push(`${stop.value.taskRoute}/${task.uuid}`)
  } else {
    alert('Denne oppgaven er låst. Fullfør tidligere oppgaver for å låse opp.')
  }
}
</script>

<template>
  <SceneComponent
    v-if="stop"
    :title="stop.name"
    :previous="previousRoute"
    :background="stop.background"
  >
    <div class="task-list">
      <div class="stop-header">
        <h2 class="stop-title">{{ stop.name }}</h2>
        <p class="stop-intro">{{ stop.intro }}</p>
      </div>

      <div v-if="isLoading" class="loading">Laster oppgaver...</div>

      <template v-else>
        <div v-if="availableTasks.length === 0" class="no-tasks">
          Ingen oppgaver tilgjengelig ennå. Fullfør tidligere oppgaver for å låse opp nye.
        </div>
        <button
          v-for="task in availableTasks"
          :key="task.taskId"
          class="task-btn btn-bare"
          :class="{
            'task-btn--completed': stopStore.isComplete(stopId, task.uuid),
          }"
          @click="goToTask(task.taskId)"
        >
          <span class="task-number">{{ task.taskId }}</span>
          <span class="task-body">
            <span class="task-title">{{ task.title }}</span>
            <span v-if="(stop.taskHints as Record<string, {hint?: string}> | undefined)?.[String(task.taskId)]?.hint && !stopStore.isComplete(stopId, task.uuid)" class="task-hint">
              {{ (stop.taskHints as Record<string, {hint?: string}>)?.[String(task.taskId)]?.hint }}
            </span>
          </span>
          <span class="task-status">
            <template v-if="stopStore.isComplete(stopId, task.uuid)">&#10003;</template>
            <template v-else>&rarr;</template>
          </span>
        </button>

        <div v-if="lockedTasks.length > 0" class="locked-tasks">
          <div
            v-for="task in lockedTasks"
            :key="task.taskId"
            class="task-btn task-btn--locked"
          >
            <span class="task-number">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
            </span>
            <span class="task-body">
              <span class="task-title">{{ task.title }}</span>
            </span>
            <span class="task-status">🔒</span>
          </div>
        </div>
      </template>
    </div>
  </SceneComponent>

  <div v-else class="not-found">
    <p>Stoppested ikke funnet.</p>
  </div>
</template>

<style scoped>
.task-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.25rem;
  padding: 2rem;
}

.stop-header {
  max-width: 520px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.stop-title {
  font-size: clamp(1.75rem, 4vw, 2.5rem);
  font-weight: 800;
  color: #ffffff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.7);
  margin: 0;
}

.stop-intro {
  font-size: 1rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.7);
  line-height: 1.5;
  margin: 0;
}

.loading {
  color: white;
  font-size: 1.1rem;
  font-weight: 600;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

.no-tasks {
  color: rgba(255, 255, 255, 0.8);
  font-size: 1.1rem;
  font-weight: 500;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
  text-align: center;
  max-width: 520px;
  line-height: 1.5;
}

.task-btn {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 100%;
  max-width: 520px;
  padding: 1.25rem 1.5rem;
  border-radius: 14px;
  border: 3px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(6px);
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, transform 0.1s, border-color 0.15s;
}

.task-btn:hover {
  background: rgba(255, 255, 255, 0.28);
  transform: scale(1.02);
}

.task-btn:active {
  transform: scale(0.99);
}

.task-btn--completed {
  border-color: #22c55e;
  background: rgba(34, 197, 94, 0.2);
}

.task-btn--locked {
  opacity: 0.5;
  cursor: not-allowed;
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.05);
}

.locked-tasks {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  width: 100%;
  max-width: 520px;
}

.task-number {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1rem;
  color: white;
  flex-shrink: 0;
}

.task-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  text-align: left;
}

.task-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: white;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

.task-hint {
  font-size: 0.8rem;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.75);
  font-style: italic;
  line-height: 1.3;
}

.task-status {
  font-size: 1.25rem;
  color: white;
  flex-shrink: 0;
}

.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  color: white;
  font-size: 1.5rem;
}
</style>
