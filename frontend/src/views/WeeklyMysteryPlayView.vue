<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import MysteryService from '@/services/MysteryService'
import type { MysteryPlayDTO } from '@/types'

const route = useRoute()
const router = useRouter()

const classroomId = computed(() => String(route.params.classroomId))
const mysteries = ref<MysteryPlayDTO[]>([])
const isLoading = ref(false)
const errorMessage = ref('')

onMounted(loadMysteries)

async function loadMysteries() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    mysteries.value = await MysteryService.getPlayList(classroomId.value)
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : 'Kunne ikke laste mysterier.'
  } finally {
    isLoading.value = false
  }
}

function goToDetail(submissionId: string) {
  router.push({
    name: 'student-weekly-mystery-detail',
    params: { classroomId: classroomId.value, submissionId },
  })
}

const previousPath = computed(
  () => `/student/classrooms/${classroomId.value}/mystery?skip=1`,
)

function goBack() {
  router.push({
    name: 'student-weekly-mystery',
    params: { classroomId: classroomId.value },
    query: { skip: '1' },
  })
}
</script>

<template>
  <SceneComponent
    title="Ukens Mysterium"
    :previous="previousPath"
    background="/CorridorBackground.jpeg"
    @back="goBack"
  >
    <div class="task-list">
      <div class="stop-header">
        <h2 class="stop-title">Ukens Mysterium</h2>
        <p class="stop-intro">
          Velg et mysterium og finn det riktige svaret. Hvert løste mysterium gir deg en utmerkelse.
        </p>
      </div>

      <div v-if="isLoading" class="loading">Laster mysterier...</div>
      <p v-else-if="errorMessage" class="state-msg state-msg--error">{{ errorMessage }}</p>

      <template v-else>
        <div v-if="mysteries.length === 0" class="no-tasks">
          Ingen godkjente mysterier ennå. Send inn et selv eller vent på at klassekamerater
          gjør det.
        </div>

        <button
          v-for="(mystery, index) in mysteries"
          :key="mystery.id"
          class="task-btn btn-bare"
          :class="{ 'task-btn--completed': mystery.completed }"
          @click="goToDetail(mystery.id)"
        >
          <span class="task-number">{{ index + 1 }}</span>
          <span class="task-body">
            <span class="task-title">Mysterium #{{ index + 1 }}</span>
          </span>
          <span class="task-status">
            <template v-if="mystery.completed">&#10003;</template>
            <template v-else>&rarr;</template>
          </span>
        </button>
      </template>
    </div>
  </SceneComponent>
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
  color: rgba(255, 255, 255, 0.85);
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

.task-btn:active { transform: scale(0.99); }

.task-btn--completed {
  border-color: #22c55e;
  background: rgba(34, 197, 94, 0.2);
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
  text-align: left;
}

.task-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: white;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

.task-status {
  font-size: 1.25rem;
  color: white;
  flex-shrink: 0;
}

.state-msg {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: white;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

.state-msg--error { color: #fca5a5; }
</style>
