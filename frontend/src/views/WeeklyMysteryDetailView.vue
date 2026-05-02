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
const flashIndex = ref<number | null>(null)

const LABELS = ['A', 'B', 'C', 'D']
const TONE_VARS = [
  'var(--color-nd-glow-cool)',
  'var(--color-nd-accent)',
  'var(--color-nd-glow)',
  'var(--color-nd-success)',
]

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
  flashIndex.value = selectedIndex
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
      router.push({
        name: 'student-weekly-mystery-play',
        params: { classroomId: classroomId.value },
      })
    }
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : 'Noe gikk galt.'
  } finally {
    isSubmitting.value = false
    flashIndex.value = null
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
    :lock-viewport="true"
    @back="goBack"
  >
    <div class="mystery-task">
      <div class="task-header">
        <h2 class="question">Hva er sant om dette?</h2>
      </div>

      <p v-if="isLoading" class="state-msg">Laster mysterium...</p>
      <p v-else-if="errorMessage" class="state-msg state-msg--error">{{ errorMessage }}</p>

      <div v-else-if="mystery" class="mystery-body">
        <div class="photo-frame">
          <img class="photo-image" :src="mystery.imagePath" alt="Mysterium" />
        </div>

        <div class="alternatives">
          <button
            v-for="(text, index) in mystery.alternatives"
            :key="index"
            type="button"
            class="alt-btn"
            :class="{
              'alt-btn--wrong': wrongIndex === index,
              'alt-btn--solved': mystery.completed,
              'is-flashing': flashIndex === index,
            }"
            :style="{ '--ring-color': TONE_VARS[index] ?? 'var(--color-nd-glow)' }"
            :disabled="isSubmitting || mystery.completed"
            @click="pickAlternative(index)"
          >
            <span class="alt-tag">{{ LABELS[index] ?? index + 1 }}</span>
            <span class="alt-text">{{ text }}</span>
          </button>
        </div>

        <p v-if="mystery.completed" class="solved-msg">
          Mysteriet er løst!
        </p>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.mystery-task {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0.5rem 1.5rem 1rem;
  min-height: 0;
  overflow: hidden;
}

.task-header {
  flex-shrink: 0;
  text-align: center;
  margin-bottom: 0.75rem;
}

.question {
  color: white;
  font-size: clamp(1.5rem, 3vw, 2.5rem);
  font-weight: 900;
  letter-spacing: 0.01em;
  margin: 0;
  text-shadow: 0 3px 10px rgba(0, 0, 0, 0.85);
}

.mystery-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  align-items: center;
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  min-height: 0;
}

.photo-frame {
  width: 100%;
  flex: 1;
  min-height: 0;
  border-radius: 12px;
  background: #0a0a14;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.55);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.alternatives {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  width: 100%;
  flex-shrink: 0;
}

.alt-btn {
  --ring-color: var(--color-nd-glow);
  display: flex;
  align-items: center;
  gap: 0.85rem;
  width: 100%;
  padding: 0.85rem 1.1rem;
  text-align: left;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.alt-btn:not(:disabled):hover,
.alt-btn:focus-visible {
  transform: scale(1.02);
}

.alt-btn.is-flashing::after {
  background: var(--ring-color);
}

.alt-btn--wrong::before { background: var(--color-nd-button-danger-edge); }
.alt-btn--wrong::after { background: #fee2e2; }
.alt-btn--wrong { color: #991b1b; animation: shake 0.32s; }
.alt-btn--wrong .alt-tag { background: var(--color-nd-button-danger-edge); color: #ffffff; }

.alt-btn--solved::before { background: #166534; }
.alt-btn--solved::after { background: #f0fdf4; }
.alt-btn--solved { color: #166534; }
.alt-btn--solved .alt-tag { background: #166534; color: #ffffff; }

.alt-tag {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  background: var(--ring-color);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 0.95rem;
  flex-shrink: 0;
  -webkit-text-stroke: 2px #000000;
  paint-order: stroke fill;
  box-shadow: 0 0 0 2px #000000;
}

.alt-text {
  flex: 1;
  min-width: 0;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-7px); }
  40% { transform: translateX(7px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.state-msg {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: white;
  text-align: center;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.7);
}

.state-msg--error { color: #fca5a5; }

.solved-msg {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
  color: #ffffff;
  background: rgba(22, 101, 52, 0.85);
  padding: 0.6rem 1rem;
  border-radius: 8px;
  text-align: center;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

@media (max-width: 1100px) {
  .mystery-task { padding-inline: 1rem; }
}

@media (max-width: 800px) {
  .mystery-task {
    overflow: visible;
    min-height: 0;
  }
  .photo-frame {
    flex: none;
    aspect-ratio: 4 / 3;
  }
}
</style>
