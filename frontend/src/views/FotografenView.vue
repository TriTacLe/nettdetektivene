<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import GuidanceComponent from '../components/GuidanceComponent.vue'
import { useStopStore } from '@/stores/StopStore'

const STOP_ID = 'fotografen'

const router = useRouter()
const stopStore = useStopStore()

const testClassroomId = stopStore.testModeClassroomId
const previousRoute = computed(() =>
  testClassroomId ? `/classroom/${testClassroomId}/test-game` : '/game-map'
)

const tasks = computed(() => stopStore.tasks[STOP_ID] ?? [])
const isLoading = computed(() => stopStore.loading[STOP_ID] ?? false)
const availableTasks = computed(() => tasks.value.filter(t => t.isUnlocked))
const lockedTasks = computed(() => tasks.value.filter(t => !t.isUnlocked))

onMounted(() => {
  if (testClassroomId) {
    stopStore.loadTasksTestMode(testClassroomId, STOP_ID)
  } else {
    stopStore.clearTasks(STOP_ID)
    stopStore.loadTasks(STOP_ID, true)
  }
})

function selectTask(uuid: string, taskId: number) {
  if (!tasks.value.find(t => t.taskId === taskId)?.isUnlocked) {
    alert('Denne oppgaven er låst. Fullfør tidligere oppgaver for å låse opp.')
    return
  }
  router.push({
    name: 'fotografen-task',
    params: { taskId: uuid },
    query: testClassroomId ? { classroomId: testClassroomId } : {},
  })
}
</script>

<template>
  <SceneComponent title="Fotografen" :previous="previousRoute" background="/FotografenBackground.png">
    <div class="list-container">
      <GuidanceComponent tip="Her skal du lære å skille ekte bilder fra KI-genererte og manipulerte bilder. Se etter unaturlige detaljer, rare skygger og uskarpe kanter!" />
      <div v-if="isLoading" class="state-msg">Laster oppgaver...</div>
      <div v-else-if="tasks.length === 0" class="state-msg state-msg--empty">
        Ingen oppgaver funnet for Fotografen
      </div>
      <div v-else class="task-grid">
        <button
          v-for="task in availableTasks"
          :key="task.taskId"
          class="task-card"
          :class="{ 'task-card--completed': stopStore.isComplete(STOP_ID, task.uuid) }"
          @click="selectTask(task.uuid, task.taskId)"
        >
          <div class="card-number">{{ task.taskId }}</div>
          <div class="card-body">
            <h3 class="card-title">{{ task.title }}</h3>
            <div class="card-meta">
              <span class="badge badge-type">{{ task.taskType }}</span>
            </div>
          </div>
          <div class="card-action">
            <span v-if="stopStore.isComplete(STOP_ID, task.uuid)" class="play-btn">&#10003;</span>
            <span v-else class="play-btn">&#9654;</span>
          </div>
        </button>

        <div
          v-for="task in lockedTasks"
          :key="task.taskId"
          class="task-card task-card--locked"
        >
          <div class="card-number">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ task.title }}</h3>
          </div>
          <div class="card-action">
            <span class="locked-label">Låst</span>
          </div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.list-container {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
}

.state-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  flex: 1;
  color: white;
  font-size: 1.3rem;
  font-weight: 800;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.8);
  text-align: center;
  min-height: 200px;
}

.state-msg--error { color: #fca5a5; }
.state-msg--empty { color: rgba(255, 255, 255, 0.7); }

.task-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.task-card {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  background: rgba(253, 250, 244, 0.95);
  border: 2px solid rgba(255, 215, 0, 0.35);
  border-radius: 14px;
  padding: 1.25rem 1.5rem;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.35);
  width: 100%;
}

.task-card:hover:not(:disabled) {
  border-color: #FFD700;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
}

.task-card:active:not(:disabled) { transform: translateY(0); }

.task-card--locked {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.task-card--completed {
  border-color: #22c55e;
  background: rgba(34, 197, 94, 0.15);
}

.card-number {
  flex-shrink: 0;
  width: 2.75rem;
  height: 2.75rem;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  font-weight: 900;
  color: #1a1a1a;
  box-shadow: 0 2px 6px rgba(255, 165, 0, 0.4);
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.card-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  color: white;
  -webkit-text-stroke: 5px #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  align-items: center;
}

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.badge-type {
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: #1a1a1a;
  border: 1px solid rgba(255, 215, 0, 0.5);
}


.card-action { flex-shrink: 0; }

.play-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  background: #1a1a1a;
  color: white;
  border-radius: 50%;
  font-size: 1rem;
  padding-left: 0.15rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: background 0.15s;
}

.task-card:hover:not(:disabled) .play-btn {
  background: #FFD700;
  color: #1a1a1a;
}

.locked-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: #999;
  text-transform: uppercase;
}

@media (max-width: 600px) {
  .list-container { padding: 1rem; }
  .card-title { font-size: 1rem; }
  .task-card { padding: 1rem; }
}
</style>
