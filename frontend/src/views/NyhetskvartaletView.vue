<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import { fetchTasksByStopLocation } from '@/services/TaskService'
import type { TaskDTO } from '@/types'

const router = useRouter()
const tasks = ref<TaskDTO[]>([])
const isLoading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    tasks.value = await fetchTasksByStopLocation('nyhetskvartalet')
  } catch (err) {
    error.value = 'Kunne ikke laste oppgaver. Prøv igjen.'
    console.error(err)
  } finally {
    isLoading.value = false
  }
})

function selectTask(task: TaskDTO) {
  router.push({ name: 'nyhetskvartalet-task', params: { taskId: task.id } })
}
</script>

<template>
  <SceneComponent title="Nyhetskvartalet" previous="/game-map" background="/NyhetskvartaletBackground.png">
    <div class="list-container">
      <div v-if="isLoading" class="state-msg">Laster oppgaver...</div>
      <div v-else-if="error" class="state-msg state-msg--error">{{ error }}</div>
      <div v-else-if="tasks.length === 0" class="state-msg state-msg--empty">
        Ingen oppgaver funnet for Nyhetskvartalet
      </div>
      <div v-else class="task-grid">
        <button
          v-for="(task, index) in tasks"
          :key="task.id"
          class="task-card"
          :class="{ 'task-card--inactive': !task.isActive }"
          :disabled="!task.isActive"
          @click="selectTask(task)"
        >
          <div class="card-number">{{ index + 1 }}</div>
          <div class="card-body">
            <h3 class="card-title">{{ task.title }}</h3>
            <div class="card-meta">
              <span class="badge badge-type">{{ task.taskType }}</span>
              <span class="badge badge-stop">{{ task.stopLocation }}</span>
              <span class="badge badge-diff">
                <span v-for="n in 5" :key="n" class="star" :class="{ 'star--filled': n <= task.difficulty }">★</span>
              </span>
            </div>
          </div>
          <div class="card-action">
            <span v-if="task.isActive" class="play-btn">&#9654;</span>
            <span v-else class="locked-label">Inaktiv</span>
          </div>
        </button>
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

.task-card--inactive {
  opacity: 0.5;
  cursor: not-allowed;
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

.badge-stop {
  background: rgba(0, 0, 0, 0.08);
  color: #555;
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.badge-diff {
  background: transparent;
  border: none;
  padding: 0;
  display: flex;
  gap: 0.1rem;
}

.star { color: #ccc; font-size: 1rem; }
.star--filled { color: #FFD700; }

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
