<script setup lang="ts">
import { ref, onMounted } from 'vue'
import SceneComponent from '@/components/SceneComponent.vue'
import { fetchAllTasks } from '@/services/TaskService'
import type { TaskDTO } from '@/types'

const tasks = ref<TaskDTO[]>([])
const isLoading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    tasks.value = await fetchAllTasks()
  } catch (err) {
    error.value = 'Kunne ikke laste oppgaver. Prøv igjen.'
    console.error(err)
  } finally {
    isLoading.value = false
  }
})

const formatContent = (content: Record<string, unknown> | null) => {
  if (!content) return 'Ingen innhold'
  return JSON.stringify(content, null, 2)
}
</script>

<template>
  <SceneComponent title="Alle Oppgaver" previous="/teacher-home" background="/StartBackground.jpg">
    <div class="tasks-container">
      <div v-if="isLoading" class="state-msg">
        Laster oppgaver...
      </div>
      <div v-else-if="error" class="state-msg state-msg--error">
        {{ error }}
      </div>
      <div v-else-if="tasks.length === 0" class="state-msg state-msg--empty">
        Ingen oppgaver funnet i databasen
      </div>
      <div v-else class="tasks-list">
        <div v-for="task in tasks" :key="task.id" class="task-card">
          <div class="task-header">
            <h3 class="task-title">{{ task.title }}</h3>
            <div class="task-badges">
              <span class="badge badge-type">{{ task.taskType }}</span>
              <span class="badge badge-difficulty">{{ task.difficulty }}</span>
              <span v-if="task.isActive" class="badge badge-active">Aktiv</span>
              <span v-else class="badge badge-inactive">Inaktiv</span>
            </div>
          </div>
          <div class="task-info">
            <p class="info-item"><span class="info-label">ID:</span> {{ task.id }}</p>
            <p class="info-item"><span class="info-label">Stop Location:</span> {{ task.stopLocation || 'Ikke satt' }}</p>
          </div>
          <div class="task-content">
            <label class="content-label"><strong>Innhold:</strong></label>
            <pre class="content-box">{{ formatContent(task.content) }}</pre>
          </div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.tasks-container {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
  max-width: 1100px;
  margin: 0 auto;
  width: 100%;
}

.state-msg {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.3rem;
  font-weight: 800;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.8);
  text-align: center;
}

.state-msg--error {
  color: #fca5a5;
}

.state-msg--empty {
  color: rgba(255, 255, 255, 0.7);
}

.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.task-card {
  display: flex;
  flex-direction: column;
  background: #fdfaf4;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  border: 2px solid rgba(255, 215, 0, 0.3);
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.task-card:hover {
  border-color: #FFD700;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.25rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.task-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 800;
  color: #1a1a1a;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.task-badges {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.badge {
  padding: 0.4rem 1rem;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 700;
  text-transform: uppercase;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.badge-type {
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);
  color: #1a1a1a;
  border: 1px solid rgba(255, 215, 0, 0.5);
}

.badge-difficulty {
  background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);
  color: #5d4037;
  border: 1px solid rgba(255, 215, 0, 0.4);
}

.badge-active {
  background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
  color: #155724;
  border: 1px solid rgba(40, 167, 69, 0.3);
}

.badge-inactive {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  color: #6c757d;
  border: 1px solid rgba(108, 117, 125, 0.3);
}

.task-info {
  margin-bottom: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-item {
  margin: 0;
  font-size: 1rem;
  color: #2d2d2d;
  font-weight: 500;
}

.info-label {
  font-weight: 700;
  color: #FFD700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.task-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.content-label {
  font-weight: 700;
  color: #1a1a1a;
  font-size: 1rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.content-box {
  background: #ffffff;
  border: 2px solid rgba(255, 215, 0, 0.3);
  border-radius: 8px;
  padding: 1rem;
  font-size: 0.9rem;
  color: #2d2d2d;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 300px;
  overflow-y: auto;
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.05);
}

@media (max-width: 768px) {
  .tasks-container {
    padding: 1rem;
  }

  .task-header {
    flex-direction: column;
  }

  .task-title {
    font-size: 1.25rem;
  }

  .task-card {
    padding: 1rem;
  }

  .badge {
    font-size: 0.75rem;
    padding: 0.3rem 0.75rem;
  }
}
</style>
