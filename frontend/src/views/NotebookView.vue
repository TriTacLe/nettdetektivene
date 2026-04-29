<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import {
  createNotebookMutation,
  getMyNotebooksOptions,
  getMyNotebooksQueryKey,
  updateNotebookMutation,
} from '@/generated-client/@tanstack/vue-query.gen'
import type { NotebookEntryResponseDto } from '@/generated-client/types.gen'
import SceneComponent from '@/components/SceneComponent.vue'
import { TaskType } from '@/types'

const route = useRoute()
const queryClient = useQueryClient()

const STOP_LABELS: Record<TaskType, string> = {
  [TaskType.NEWS]: 'Nyhetskvartalet',
  [TaskType.PHOTO]: 'Fotografen',
  [TaskType.EMAIL]: 'Postkontoret',
  [TaskType.PASSWORD]: 'Passordbanken',
  [TaskType.MARKETPLACE]: 'Markedsplassen',
  [TaskType.SOCIAL]: 'Sosiale møteplassen',
  [TaskType.BOSS]: 'Datasenteret',
}

const STOP_LOCATION_KEY: Record<TaskType, string> = {
  [TaskType.NEWS]: 'nyhetskvartalet',
  [TaskType.PHOTO]: 'fotografen',
  [TaskType.EMAIL]: 'postkontoret',
  [TaskType.PASSWORD]: 'passordbanken',
  [TaskType.MARKETPLACE]: 'markedsplassen',
  [TaskType.SOCIAL]: 'sosiale',
  [TaskType.BOSS]: 'datasenteret',
}

const BACKEND_TO_TASK_TYPE: Record<string, TaskType> = Object.fromEntries(
  Object.entries(STOP_LOCATION_KEY).map(([taskType, backendKey]) => [backendKey, taskType as TaskType]),
)

const classroomId = computed(() => route.query.classroomId as string | undefined)

const reflections = ref<Record<string, string>>({})
const savingStop = ref<string | null>(null)
const savedStop = ref<string | null>(null)
const saveError = ref<string | null>(null)
const currentSpread = ref(0)

const stops = computed(() =>
  Object.values(TaskType).filter((t) => !!entriesByStop.value[t]),
)
const totalSpreads = computed(() => Math.ceil(stops.value.length / 2))
const leftStop = computed(() => stops.value[currentSpread.value * 2] ?? null)
const rightStop = computed(() => stops.value[currentSpread.value * 2 + 1] ?? null)

const { data, isLoading, error } = useQuery(
  computed(() =>
    getMyNotebooksOptions({
      path: { classroomId: classroomId.value ?? '' },
    }),
  ),
)

const displayError = computed(() => {
  if (!classroomId.value) return 'Mangler klasserom-ID. Naviger hit fra klasserommet ditt.'
  return error.value ? 'Kunne ikke hente notatblokkoppføringer' : null
})

const entriesByStop = computed(() => {
  const entries = data.value?.data ?? []
  return entries.reduce<Record<string, NotebookEntryResponseDto>>((acc, entry) => {
    if (entry.stopLocation) {
      const taskType = BACKEND_TO_TASK_TYPE[entry.stopLocation]
      if (taskType) acc[taskType] = entry
    }
    return acc
  }, {})
})

const createMutation = useMutation(createNotebookMutation())
const updateMutation = useMutation(updateNotebookMutation())

const saveMutation = useMutation({
  mutationFn: async (payload: {
    stopLocation: string
    studentReflection: string
    autoNotes?: string
  }) => {
    const taskType = BACKEND_TO_TASK_TYPE[payload.stopLocation]
    const existing = taskType ? entriesByStop.value[taskType] : undefined
    if (existing?.id) {
      return updateMutation.mutateAsync({
        path: { classroomId: classroomId.value!, notebookId: existing.id },
        body: payload,
      })
    }
    return createMutation.mutateAsync({
      path: { classroomId: classroomId.value! },
      body: payload,
    })
  },
  onSuccess: () => {
    queryClient.invalidateQueries({
      queryKey: getMyNotebooksQueryKey({ path: { classroomId: classroomId.value ?? '' } }),
    })
    savedStop.value = savingStop.value
    savingStop.value = null
    setTimeout(() => {
      savedStop.value = null
    }, 2000)
  },
  onError: (e: Error) => {
    saveError.value = e?.message ?? 'Kunne ikke lagre'
    savingStop.value = null
  },
})

function prevSpread(): void {
  if (currentSpread.value > 0) currentSpread.value--
}

function nextSpread(): void {
  if (currentSpread.value < totalSpreads.value - 1) currentSpread.value++
}

function getAutoNotes(stopLocation: string): string[] {
  const raw = entriesByStop.value[stopLocation]?.autoNotes
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : [raw]
  } catch {
    return [raw]
  }
}

function getReflection(stopLocation: string): string {
  if (reflections.value[stopLocation] !== undefined) {
    return reflections.value[stopLocation]
  }
  return entriesByStop.value[stopLocation]?.studentReflection ?? ''
}

function onReflectionInput(stopLocation: string, value: string): void {
  reflections.value[stopLocation] = value
}

function saveReflection(stopLocation: string): void {
  savingStop.value = stopLocation
  savedStop.value = null
  saveError.value = null
  saveMutation.mutate({
    stopLocation: STOP_LOCATION_KEY[stopLocation as TaskType] ?? stopLocation,
    autoNotes: entriesByStop.value[stopLocation]?.autoNotes ?? undefined,
    studentReflection: reflections.value[stopLocation] ?? '',
  })
}
</script>

<template>
  <SceneComponent title="Dine notater" previous="/student-home" previous-label="Hjem">
    <div class="notebook-wrapper">
      <p v-if="isLoading" class="notebook-status" aria-live="polite">
        Laster inn notatblokk...
      </p>

      <p v-else-if="displayError" class="notebook-error" role="alert">
        {{ displayError }}
      </p>

      <template v-else>
        <p class="notebook-hint">
          Flere notater låses opp etter hvert som du fullfører nye stoppesteder i spillet.
        </p>

        <p v-if="!data?.data?.length" class="notebook-empty">
          Du har ikke fullført noen stoppesteder enda. Tips vil dukke opp her når du spiller!
        </p>

        <template v-else>
          <div class="book-nav">
            <button
              class="nav-button"
              :disabled="currentSpread === 0"
              aria-label="Forrige side"
              @click="prevSpread"
            >
              ◀
            </button>
            <span class="page-indicator">{{ currentSpread + 1 }} / {{ totalSpreads }}</span>
            <button
              class="nav-button"
              :disabled="currentSpread === totalSpreads - 1"
              aria-label="Neste side"
              @click="nextSpread"
            >
              ▶
            </button>
          </div>

          <div class="book">
            <div
              v-if="leftStop"
              class="book-page book-page--left"
            >
              <div class="card-header">
                <h2 class="stop-title">{{ STOP_LABELS[leftStop] }}</h2>
              </div>

              <div class="lined-section">
                <p v-for="(note, i) in getAutoNotes(leftStop)" :key="i" class="lined-row">
                  {{ note }}
                </p>
                <p v-if="getAutoNotes(leftStop).length === 0" class="lined-row lined-row--empty">
                  Ingen tips enda...
                </p>
                <div class="reflection-divider">✏️ Dine egne notater</div>
                <textarea
                  :id="`reflection-${leftStop}`"
                  class="reflection-input"
                  :value="getReflection(leftStop)"
                  placeholder="Skriv her..."
                  :aria-label="`Refleksjon for ${STOP_LABELS[leftStop]}`"
                  @input="onReflectionInput(leftStop, ($event.target as HTMLTextAreaElement).value)"
                />
              </div>
              <p v-if="saveError && savingStop === null && savedStop !== leftStop" class="text-red-600 text-xs font-semibold mt-1">
                {{ saveError }}
              </p>
              <button
                class="save-button"
                :disabled="savingStop === leftStop"
                :aria-label="`Lagre refleksjon for ${STOP_LABELS[leftStop]}`"
                @click="saveReflection(leftStop)"
              >
                <span v-if="savingStop === leftStop">Lagrer...</span>
                <span v-else-if="savedStop === leftStop">✓ Lagret!</span>
                <span v-else>Lagre</span>
              </button>
            </div>

            <div class="book-spine" />

            <div
              v-if="rightStop"
              class="book-page book-page--right"
            >
              <div class="card-header">
                <h2 class="stop-title">{{ STOP_LABELS[rightStop] }}</h2>
              </div>

              <div class="lined-section">
                <p v-for="(note, i) in getAutoNotes(rightStop)" :key="i" class="lined-row">
                  {{ note }}
                </p>
                <p
                  v-if="getAutoNotes(rightStop).length === 0"
                  class="lined-row lined-row--empty"
                >
                  Ingen tips enda...
                </p>
                <div class="reflection-divider">✏️ Dine egne notater</div>
                <textarea
                  :id="`reflection-${rightStop}`"
                  class="reflection-input"
                  :value="getReflection(rightStop)"
                  placeholder="Skriv her..."
                  :aria-label="`Refleksjon for ${STOP_LABELS[rightStop]}`"
                  @input="
                    onReflectionInput(
                      rightStop,
                      ($event.target as HTMLTextAreaElement).value,
                    )
                  "
                />
              </div>
              <p v-if="saveError && savingStop === null && savedStop !== rightStop" class="text-red-600 text-xs font-semibold mt-1">
                {{ saveError }}
              </p>
              <button
                class="save-button"
                :disabled="savingStop === rightStop"
                :aria-label="`Lagre refleksjon for ${STOP_LABELS[rightStop]}`"
                @click="saveReflection(rightStop)"
              >
                <span v-if="savingStop === rightStop">Lagrer...</span>
                <span v-else-if="savedStop === rightStop">✓ Lagret!</span>
                <span v-else>Lagre</span>
              </button>
            </div>

            <div v-else class="book-page book-page--right book-page--blank" aria-hidden="true" />
          </div>
        </template>
      </template>
    </div>
  </SceneComponent>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Caveat:wght@400;600&display=swap');

.notebook-wrapper {
  padding: 16px 24px 40px;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.notebook-status,
.notebook-empty {
  color: white;
  text-align: center;
  font-size: 1rem;
  margin-top: 40px;
}

.notebook-hint {
  color: #f5e7c7;
  text-align: center;
  font-size: 0.98rem;
  max-width: 42rem;
  margin: 0;
}

.notebook-error {
  color: #ff6b6b;
  text-align: center;
  font-size: 1rem;
  margin-top: 40px;
}

/* Navigation */
.book-nav {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-button {
  background: #e8a020;
  color: white;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-button:hover:not(:disabled) {
  background: #c47a10;
}

.nav-button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-indicator {
  color: white;
  font-family: 'Caveat', cursive;
  font-size: 1.1rem;
  min-width: 48px;
  text-align: center;
}

/* Open book */
.book {
  display: flex;
  width: 100%;
  box-shadow:
    0 14px 40px rgba(0, 0, 0, 0.45),
    0 4px 12px rgba(0, 0, 0, 0.22);
  border-radius: 8px;
  overflow: hidden;
}

/* Spine */
.book-spine {
  width: 12px;
  flex-shrink: 0;
  background: linear-gradient(to right, #9f620d, #e8a020 45%, #b07010 60%, #8f580c);
  box-shadow:
    inset -2px 0 4px rgba(0, 0, 0, 0.22),
    inset 2px 0 4px rgba(255, 255, 255, 0.08);
}

/* Pages */
.book-page {
  flex: 1;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.22), transparent 38%),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.08), rgba(0, 0, 0, 0.025)),
    #f8f1df;
  display: flex;
  flex-direction: column;
  min-height: 480px;
  font-family: 'Caveat', 'Courier New', cursive;
  overflow: hidden;
  position: relative;
}

.book-page::before {
  content: '';
  display: block;
  height: 14px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0.08));
  border-bottom: 1px solid rgba(120, 90, 40, 0.12);
}

.book-page--left {
  border-radius: 4px 0 0 4px;
  border: 2px solid #e8a020;
  border-right: none;
}

.book-page--right {
  border-radius: 0 4px 4px 0;
  border: 2px solid #e8a020;
  border-left: none;
}

.book-page--locked {
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.16), transparent 38%),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.06), rgba(0, 0, 0, 0.03)),
    #f3ead7;
}

.book-page--blank {
  background-color: #f5edd6;
}

/* Header */
.card-header {
  background: transparent;
  padding: 14px 20px 10px;
  box-shadow: none;
}

.card-header::after {
  content: '';
  display: block;
  margin-top: 8px;
  height: 1px;
  background: #e8a020;
  opacity: 0.7;
}
.stop-title {
  font-family: 'Caveat', cursive;
  font-size: 1.3rem;
  font-weight: 400;
}

/* Locked */
.locked-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
  gap: 10px;
  flex: 1;
  text-align: center;
  position: relative;
}

.locked-content::before {
  content: '';
  position: absolute;
  inset: 24px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  pointer-events: none;
}

.lock-icon {
  font-size: 2.2rem;
  position: relative;
  z-index: 1;
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.15));
}

.stop-locked {
  color: #7f7a70;
  font-size: 1rem;
  font-style: italic;
  margin: 0;
  font-family: 'Caveat', cursive;
  position: relative;
  z-index: 1;
}

/* Lined section */
.lined-section {
  flex: 1;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  background-image: repeating-linear-gradient(
    to bottom,
    transparent 0,
    transparent 31px,
    rgba(170, 196, 226, 0.75) 31px,
    rgba(170, 196, 226, 0.75) 32px
  );
  background-size: 100% 32px;
}

/* Printed notes */
.lined-row {
  font-family: 'Caveat', cursive;
  font-size: 1rem;
  color: #333;
  margin: 0;
  height: 32px;
  line-height: 32px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lined-row--empty {
  color: #aaa;
  font-style: italic;
}

/* Divider label between auto notes and reflection */
.reflection-divider {
  font-family: 'Caveat', cursive;
  font-size: 0.95rem;
  font-weight: 600;
  color: #e8a020;
  height: 32px;
  line-height: 32px;
  margin: 0;
  padding: 0;
  border-top: 1px solid #e8a020;
  opacity: 0.7;
}

/* Reflection textarea */
.reflection-input {
  flex: 1;
  width: 100%;
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  color: #333;
  font-size: 1rem;
  line-height: 32px;
  font-family: 'Caveat', cursive;
  resize: none;
  box-sizing: border-box;
  outline: none;
  overflow-y: auto;
}

.reflection-input::placeholder {
  color: #bbb;
  font-style: italic;
}

/* Save button */
.save-button {
  align-self: flex-end;
  background: linear-gradient(to bottom, #eba92c, #dc9619);
  color: white;
  border: none;
  border-radius: 999px;
  padding: 8px 20px;
  margin: 4px 16px 12px;
  min-width: 74px;
  font-size: 1rem;
  font-family: 'Caveat', cursive;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.2s,
    transform 0.15s,
    box-shadow 0.2s;
  box-shadow:
    0 3px 8px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.save-button:hover:not(:disabled) {
  background: linear-gradient(to bottom, #f0b43f, #d38710);
  transform: translateY(-1px);
}

.save-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

/* Mobile */
@media (max-width: 640px) {
  .book {
    flex-direction: column;
  }

  .book-spine {
    width: 100%;
    height: 8px;
    background: linear-gradient(to bottom, #b07010, #e8a020, #b07010);
  }

  .book-page--left {
    border-radius: 4px 4px 0 0;
    border: 2px solid #e8a020;
    border-bottom: none;
  }

  .book-page--right,
  .book-page--blank {
    border-radius: 0 0 4px 4px;
    border: 2px solid #e8a020;
    border-top: none;
  }
}
</style>
