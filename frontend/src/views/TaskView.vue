<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import { fetchTaskById } from '@/services/TaskService'
import { useStopStore } from '@/stores/StopStore'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId as string
const stopStore = useStopStore()
const testClassroomId = stopStore.testModeClassroomId
const previousRoute = testClassroomId
  ? `/stops/${route.params.stopId}?test=true&classroomId=${testClassroomId}`
  : `/stops/${route.params.stopId}`

const task = ref<any>(null)
const isLoading = ref(true)
const error = ref('')

onMounted(async () => {
  console.log('[TaskView] Loading task with ID:', taskId)
  const stopId = route.params.stopId as string
  console.log('[TaskView] Stop ID:', stopId)
  console.log('[TaskView] Test mode classroom ID:', testClassroomId)
  
  try {
    if (testClassroomId) {
      const stopTasks = stopStore.tasks[stopId] || []
      const taskFromStore = stopTasks.find(t => t.uuid === taskId)
      
      if (taskFromStore) {
        console.log('[TaskView] Found task in store:', taskFromStore)
        task.value = {
          id: taskFromStore.uuid,
          title: taskFromStore.title,
          taskType: taskFromStore.taskType,
          difficulty: 1,
          isActive: true,
          stopLocation: stopId,
          content: taskFromStore.content
        }
        console.log('[TaskView] Task from store content:', task.value?.content)
      } else {
        console.warn('[TaskView] Task not found in store, fetching from API')
        task.value = await fetchTaskById(taskId)
      }
    } else {
      task.value = await fetchTaskById(taskId)
    }
    
    console.log('[TaskView] Final task content:', task.value?.content)

  } catch (err) {
    error.value = 'Kunne ikke laste oppgaven. Prøv igjen.'
    console.error('[TaskView] Error loading task:', err)
  } finally {
    isLoading.value = false
  }
})

async function submitAttempt() {
  alert('Oppgaveinsending er ikke implementert ennå for denne oppgavetypen.')
}

function goBack() {
  router.push(previousRoute)
}
</script>

<template>
  <SceneComponent
    v-if="task"
    :title="task.title"
    :previous="previousRoute"
    background="/GameMapBackground.jpg"
  >
    <div class="task-container">
      <div v-if="isLoading">Laster oppgave...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="task-content">
        <h2>{{ task.title }}</h2>
        <p>Vanskelighetsgrad: {{ task.difficulty }}</p>
        <p>Oppgavetype: {{ task.taskType }}</p>
        
        <div v-if="task.content" class="content-section">
          <h3>Oppgaveinnhold:</h3>
          <pre>{{ JSON.stringify(task.content, null, 2) }}</pre>
        </div>
        
        <div v-else class="warning">
          Oppgaven mangler innhold.
        </div>
        
        <div class="actions">
          <button @click="goBack" class="back-btn">Tilbake</button>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.task-container {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

.error {
  color: red;
  text-align: center;
}

.task-content {
  background: rgba(255, 255, 255, 0.95);
  padding: 2rem;
  border-radius: 1rem;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.35);
}

.task-content h2 {
  margin-top: 0;
  color: #1a1a1a;
}

.task-content h3 {
  margin-top: 1.5rem;
  color: #333;
}

.content-section {
  margin-top: 1.5rem;
  border-top: 1px solid #ddd;
  padding-top: 1rem;
}

pre {
  background: #f5f5f5;
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
}

.warning {
  color: #666;
  font-style: italic;
  margin-top: 1rem;
}

.actions {
  margin-top: 2rem;
}

.back-btn {
  padding: 0.75rem 1.5rem;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 1rem;
}

.back-btn:hover {
  background: #45a049;
}
</style>