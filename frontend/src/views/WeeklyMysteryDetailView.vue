<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import SceneComponent from '@/components/SceneComponent.vue'
import MysteryService from '@/services/MysteryService'
import { useToast } from '@/composables/UseToast'
import { getMyAchievementsQueryKey } from '@/generated-client/@tanstack/vue-query.gen'
import type { MysteryPlayDTO } from '@/types'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const queryClient = useQueryClient()

const classroomId = computed(() => String(route.params.classroomId))
const submissionId = computed(() => String(route.params.submissionId))
const previousPath = computed(() => `/student/classrooms/${classroomId.value}/mystery/play`)

const mystery = ref<MysteryPlayDTO | null>(null)
const isLoading = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')
const wrongIndex = ref<number | null>(null)

onMounted(loadMystery)

async function loadMystery() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    mystery.value = await MysteryService.getMysteryForPlay(submissionId.value)
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : 'Kunne ikke laste mysteriet.'
  } finally {
    isLoading.value = false
  }
}

async function pickAlternative(selectedIndex: number) {
  if (!mystery.value || isSubmitting.value || mystery.value.completed) return
  isSubmitting.value = true
  wrongIndex.value = null
  try {
    const result = await MysteryService.completeMystery(submissionId.value, { selectedIndex })
    if (result.correct) {
      mystery.value = { ...mystery.value, completed: true }
      toast.showSuccess('Riktig svar!')
      await queryClient.invalidateQueries({ queryKey: getMyAchievementsQueryKey() })
      router.push({ name: 'student-weekly-mystery-play', params: { classroomId: classroomId.value } })
    } else {
      wrongIndex.value = selectedIndex
      toast.showError('Feil svar — du ryker ut av mysteriet')
      // One wrong attempt ends the run for this mystery. Send the player
      // back to Hansen's dialog at the choices (same destination as the
      // back button) so they can pick another mystery or submit a tip.
      router.push({
        name: 'student-weekly-mystery',
        params: { classroomId: classroomId.value },
        query: { skip: '1' },
      })
    }
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : 'Noe gikk galt.'
  } finally {
    isSubmitting.value = false
  }
}

function goBack() {
  router.push(previousPath.value)
}
</script>

<template>
  <SceneComponent
    title="Ukens Mysterium"
    :previous="previousPath"
    background="/CorridorBackground.jpeg"
    @back="goBack"
  >
    <div class="news-task">
      <div class="task-header">
        <h2 class="question">Hva er sant om dette?</h2>
      </div>

      <p v-if="isLoading" class="state-msg">Laster mysterium...</p>
      <p v-else-if="errorMessage" class="state-msg state-msg--error">{{ errorMessage }}</p>

      <article v-else-if="mystery" class="article-card">
        <img class="article-image" :src="mystery.imagePath" alt="Mysterium" />

        <div class="alternatives">
          <button
            v-for="(text, index) in mystery.alternatives"
            :key="index"
            type="button"
            class="article-choice btn-bare"
            :class="{
              'article-choice--wrong': wrongIndex === index,
              'article-choice--solved': mystery.completed,
            }"
            :disabled="isSubmitting || mystery.completed"
            @click="pickAlternative(index)"
          >
            <span class="alt-tag">{{ ['A', 'B', 'C'][index] }}</span>
            <span class="alt-text">{{ text }}</span>
          </button>
        </div>

        <p v-if="mystery.completed" class="solved-msg">
          Mysteriet er løst!
        </p>
      </article>
    </div>
  </SceneComponent>
</template>

<style scoped>
.news-task {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem 1.5rem 1.5rem;
  overflow-y: auto;
  min-height: 0;
}

.task-header {
  flex-shrink: 0;
  text-align: center;
}

.question {
  color: white;
  font-size: clamp(1.5rem, 3vw, 2.25rem);
  font-weight: 800;
  margin: 0;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.8);
}

.article-card {
  display: flex;
  flex-direction: column;
  background: #fdfaf4;
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  gap: 1rem;
  border: 3px solid transparent;
  width: min(640px, 100%);
  margin: 0 auto;
}

.article-image {
  width: 100%;
  max-height: min(48vh, 480px);
  object-fit: contain;
  border-radius: 6px;
  background: #e9e5da;
}

.alternatives {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.article-choice {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  width: 100%;
  padding: 0.85rem 1rem;
  border: 2px solid #1a1a1a;
  border-radius: 8px;
  background: white;
  color: #1a1a1a;
  font-family: system-ui, sans-serif;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s, transform 0.1s, border-color 0.15s;
}

.article-choice:hover:not(:disabled) {
  background: #1a1a1a;
  color: white;
}

.article-choice:hover:not(:disabled) .alt-tag {
  background: white;
  color: #1a1a1a;
}

.article-choice:active:not(:disabled) { transform: scale(0.99); }

.article-choice:disabled {
  cursor: default;
  opacity: 0.6;
}

.article-choice--wrong {
  border-color: #dc2626;
  background: #fee2e2;
  color: #991b1b;
  animation: shake 0.32s;
}

.article-choice--solved {
  border-color: #16a34a;
  background: #f0fdf4;
  color: #166534;
}

.alt-tag {
  width: 1.85rem;
  height: 1.85rem;
  border-radius: 50%;
  background: #1a1a1a;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 0.9rem;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}

.alt-text { flex: 1; }

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-7px); }
  40% { transform: translateX(7px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.state-msg {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: white;
  text-align: center;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

.state-msg--error { color: #fca5a5; }

.solved-msg {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: #166534;
  background: #f0fdf4;
  border: 1px solid #86efac;
  padding: 0.55rem 0.75rem;
  border-radius: 6px;
  text-align: center;
}

@media (max-width: 1100px) {
  .news-task { padding-inline: 1rem; }
}
</style>
