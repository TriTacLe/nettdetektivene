<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
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
import { Lock, ChevronLeft, ChevronRight } from 'lucide-vue-next'
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

const ALL_STOPS = Object.values(TaskType)

const isMobile = ref(window.innerWidth <= 1000)

function onResize() {
  const wasMobile = isMobile.value
  isMobile.value = window.innerWidth <= 1000
  if (wasMobile !== isMobile.value) currentSpread.value = 0
}
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const displayStops = computed(() =>
  ALL_STOPS.map((t) => ({ type: t, locked: !entriesByStop.value[t] })),
)

const totalSpreads = computed(() =>
  isMobile.value
    ? displayStops.value.length
    : Math.max(1, Math.ceil(displayStops.value.length / 2)),
)
const leftEntry = computed(() =>
  isMobile.value
    ? displayStops.value[currentSpread.value] ?? null
    : displayStops.value[currentSpread.value * 2] ?? null,
)
const rightEntry = computed(() =>
  isMobile.value ? null : displayStops.value[currentSpread.value * 2 + 1] ?? null,
)

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
  onError: (e: any) => {
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
          <div class="book-carousel">
            <button
              class="btn-bare carousel-arrow carousel-arrow--left"
              :disabled="currentSpread === 0"
              aria-label="Forrige side"
              @click="prevSpread"
            >
              <ChevronLeft :size="28" />
              <span>Forrige</span>
            </button>

            <div class="book">
              <template v-if="leftEntry">
                <div
                  v-if="leftEntry.locked"
                  class="book-page book-page--left book-page--locked"
                >
                  <div class="card-header card-header--locked">
                    <h2 class="stop-title">{{ STOP_LABELS[leftEntry.type] }}</h2>
                  </div>
                  <div class="locked-content">
                    <Lock class="lock-icon" :size="72" aria-hidden="true" />
                    <p class="stop-locked">Fullfør forrige stoppested for å låse opp</p>
                  </div>
                </div>
                <div v-else class="book-page book-page--left">
                  <div class="card-header">
                    <h2 class="stop-title">{{ STOP_LABELS[leftEntry.type] }}</h2>
                  </div>
                  <div class="lined-section">
                    <p v-for="(note, i) in getAutoNotes(leftEntry.type)" :key="i" class="lined-row">
                      {{ note }}
                    </p>
                    <p v-if="getAutoNotes(leftEntry.type).length === 0" class="lined-row lined-row--empty">
                      Ingen tips enda...
                    </p>
                    <div class="reflection-divider">Dine egne notater</div>
                    <textarea
                      :id="`reflection-${leftEntry.type}`"
                      class="reflection-input"
                      :value="getReflection(leftEntry.type)"
                      placeholder="Skriv her..."
                      :aria-label="`Refleksjon for ${STOP_LABELS[leftEntry.type]}`"
                      @input="onReflectionInput(leftEntry.type, ($event.target as HTMLTextAreaElement).value)"
                    />
                  </div>
                  <p v-if="saveError && savingStop === null && savedStop !== leftEntry.type" class="save-error" role="alert">
                    {{ saveError }}
                  </p>
                  <button
                    class="save-button"
                    :disabled="savingStop === leftEntry.type"
                    :aria-label="`Lagre refleksjon for ${STOP_LABELS[leftEntry.type]}`"
                    @click="saveReflection(leftEntry.type)"
                  >
                    <span v-if="savingStop === leftEntry.type">Lagrer...</span>
                    <span v-else-if="savedStop === leftEntry.type">Lagret!</span>
                    <span v-else>Lagre</span>
                  </button>
                </div>
              </template>

              <div v-if="!isMobile" class="book-spine" />

              <template v-if="rightEntry">
                <div
                  v-if="rightEntry.locked"
                  class="book-page book-page--right book-page--locked"
                >
                  <div class="card-header card-header--locked">
                    <h2 class="stop-title">{{ STOP_LABELS[rightEntry.type] }}</h2>
                  </div>
                  <div class="locked-content">
                    <Lock class="lock-icon" :size="72" aria-hidden="true" />
                    <p class="stop-locked">Fullfør forrige stoppested for å låse opp</p>
                  </div>
                </div>
                <div v-else class="book-page book-page--right">
                  <div class="card-header">
                    <h2 class="stop-title">{{ STOP_LABELS[rightEntry.type] }}</h2>
                  </div>
                  <div class="lined-section">
                    <p v-for="(note, i) in getAutoNotes(rightEntry.type)" :key="i" class="lined-row">
                      {{ note }}
                    </p>
                    <p v-if="getAutoNotes(rightEntry.type).length === 0" class="lined-row lined-row--empty">
                      Ingen tips enda...
                    </p>
                    <div class="reflection-divider">Dine egne notater</div>
                    <textarea
                      :id="`reflection-${rightEntry.type}`"
                      class="reflection-input"
                      :value="getReflection(rightEntry.type)"
                      placeholder="Skriv her..."
                      :aria-label="`Refleksjon for ${STOP_LABELS[rightEntry.type]}`"
                      @input="onReflectionInput(rightEntry.type, ($event.target as HTMLTextAreaElement).value)"
                    />
                  </div>
                  <p v-if="saveError && savingStop === null && savedStop !== rightEntry.type" class="save-error" role="alert">
                    {{ saveError }}
                  </p>
                  <button
                    class="save-button"
                    :disabled="savingStop === rightEntry.type"
                    :aria-label="`Lagre refleksjon for ${STOP_LABELS[rightEntry.type]}`"
                    @click="saveReflection(rightEntry.type)"
                  >
                    <span v-if="savingStop === rightEntry.type">Lagrer...</span>
                    <span v-else-if="savedStop === rightEntry.type">Lagret!</span>
                    <span v-else>Lagre</span>
                  </button>
                </div>
              </template>

              <div v-else-if="!isMobile" class="book-page book-page--right book-page--blank" aria-hidden="true" />
            </div>

            <button
              class="btn-bare carousel-arrow carousel-arrow--right"
              :disabled="currentSpread === totalSpreads - 1"
              aria-label="Neste side"
              @click="nextSpread"
            >
              <span>Neste</span>
              <ChevronRight :size="28" />
            </button>
          </div>

          <span class="page-indicator page-indicator--desktop">Side {{ currentSpread + 1 }} av {{ totalSpreads }}</span>

          <div class="book-mobile-nav">
            <button
              class="btn-bare mobile-nav-btn"
              :disabled="currentSpread === 0"
              aria-label="Forrige side"
              @click="prevSpread"
            >
              <ChevronLeft :size="24" />
              <span>Forrige</span>
            </button>
            <span class="page-indicator">Side {{ currentSpread + 1 }} av {{ totalSpreads }}</span>
            <button
              class="btn-bare mobile-nav-btn"
              :disabled="currentSpread === totalSpreads - 1"
              aria-label="Neste side"
              @click="nextSpread"
            >
              <span>Neste</span>
              <ChevronRight :size="24" />
            </button>
          </div>
        </template>
      </template>
    </div>
  </SceneComponent>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Fredoka:wght@600&family=Nunito:wght@400;600;700;800&family=Patrick+Hand&display=swap');

.notebook-wrapper {
  padding: 16px 24px 40px;
  width: 100%;
  max-width: 1300px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.notebook-status,
.notebook-empty {
  color: #f0e8d0;
  text-align: center;
  font-size: 1.1rem;
  margin-top: 40px;
}

.notebook-hint {
  color: #d4c4a8;
  text-align: center;
  font-size: 1.15rem;
  max-width: 42rem;
  margin: 0;
}

.notebook-error {
  color: #fca5a5;
  text-align: center;
  font-size: 1.1rem;
  margin-top: 40px;
}

/* Carousel layout */
.book-carousel {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.carousel-arrow {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.15);
  border: 2px solid rgba(255, 255, 255, 0.5);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 1.1rem;
  font-weight: 700;
  transition: background 0.2s, transform 0.15s;
}

.carousel-arrow:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.carousel-arrow:disabled {
  opacity: 0.2;
  cursor: not-allowed;
}

.page-indicator {
  color: #c8c0b4;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 1.15rem;
  font-weight: 600;
  text-align: center;
}

.page-indicator--desktop {
  display: block;
}

@media (max-width: 1000px) {
  .page-indicator--desktop {
    display: none;
  }
}

/* Open book */
.book {
  display: flex;
  width: 100%;
  box-shadow:
    0 10px 30px rgba(0, 0, 0, 0.5),
    0 2px 8px rgba(0, 0, 0, 0.3);
  border-radius: 6px;
  overflow: hidden;
}

/* Spine */
.book-spine {
  width: 20px;
  flex-shrink: 0;
  background: linear-gradient(to right, #c0b8a8, #d8d0c0 50%, #c0b8a8);
  box-shadow:
    inset -1px 0 3px rgba(0, 0, 0, 0.15),
    inset 1px 0 3px rgba(0, 0, 0, 0.1);
}

/* Pages */
.book-page {
  flex: 1;
  background: #faf8f4;
  display: flex;
  flex-direction: column;
  min-height: 560px;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  overflow: hidden;
  position: relative;
}

.book-page::before {
  display: none;
}

.book-page--left {
  border-radius: 2px 0 0 2px;
  border: none;
}

.book-page--right {
  border-radius: 0 2px 2px 0;
  border: none;
}

.book-page--locked {
  background: #f0eeea;
}

.book-page--blank {
  background: #f0eeea;
}

/* Header */
.card-header {
  background: transparent;
  padding: 16px 18px 8px;
  border-bottom: 2px solid #ddd8d0;
}

.card-header--locked {
  border-bottom-color: #e0dcd6;
}

.card-header::after {
  display: none;
}

.stop-title {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 1.5rem;
  font-weight: 800;
  color: #3a3530;
  margin: 0;
  letter-spacing: 0.03em;
  text-align: center;
  -webkit-text-stroke: 0;
}

/* Locked */
.locked-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 20px;
  gap: 12px;
  flex: 1;
  text-align: center;
}

.locked-content::before {
  display: none;
}

.lock-icon {
  color: #b8b0a8;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
}

.stop-locked {
  color: #8a8480;
  font-size: 1.2rem;
  font-style: normal;
  font-weight: 600;
  margin: 0;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
}

/* Lined section */
.lined-section {
  flex: 1;
  padding: 4px 18px;
  display: flex;
  flex-direction: column;
  background-image: repeating-linear-gradient(
    to bottom,
    transparent 0,
    transparent 33px,
    rgba(120, 90, 60, 0.2) 33px,
    rgba(120, 90, 60, 0.2) 34px
  );
  background-size: 100% 34px;
}

/* Printed notes */
.lined-row {
  font-family: 'Patrick Hand', cursive;
  font-size: 1rem;
  color: #2a2220;
  margin: 0;
  min-height: 34px;
  line-height: 34px;
  white-space: normal;
  word-break: break-word;
  letter-spacing: 0.04em;
}

.lined-row--empty {
  color: #7a7068;
  font-style: italic;
}

/* Divider label between auto notes and reflection */
.reflection-divider {
  font-family: 'Fredoka', 'Nunito', sans-serif;
  font-size: 1rem;
  font-weight: 200;
  color: #000000;
  line-height: 2;
  margin: 16px -18px 2px;
  background: transparent;
  text-align: left;
  border-bottom: 2px solid #c8c2b8;
  padding-left: 15px;
  padding-top: 15px;
  letter-spacing: 0.03em;
}

/* Reflection textarea */
.reflection-input {
  flex: 1;
  width: 100%;
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  color: #2a2220;
  font-size: 1rem;
  line-height: 34px;
  font-family: 'Patrick Hand', cursive;
  letter-spacing: 0.04em;
  resize: none;
  box-sizing: border-box;
  outline: none;
  overflow-y: auto;
}

.reflection-input:focus {
  outline: 2px solid #a0a0a0;
  outline-offset: 1px;
  border-radius: 4px;
}

.reflection-input::placeholder {
  color: #6a5e54;
  font-style: italic;
}

/* Save button */
.save-button {
  --color-nd-button: #3a3530;
  --color-nd-button-edge: #1a1510;
  --color-nd-button-text: #faf8f4;
  --color-nd-button-hover: #4a4540;
  --color-nd-button-text-hover: #ffffff;
  align-self: center;
  margin: 12px 16px 16px;
  min-width: 140px;
}

.save-error {
  color: #991b1b;
  background: #fef2f2;
  border: 2px solid #dc2626;
  border-radius: 8px;
  padding: 10px 16px;
  margin: 8px 16px;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 1.1rem;
  font-weight: 700;
  text-align: center;
  align-self: center;
}

/* Mobile */
@media (max-width: 1000px) {
  .notebook-wrapper {
    padding: 16px 70px 30px;
  }

  .book {
    flex-direction: column;
  }

  .book-carousel {
    flex-direction: column;
  }

  .carousel-arrow {
    display: none;
  }

}

.book-mobile-nav {
  display: none;
  justify-content: center;
  align-items: center;
  gap: 24px;
  margin-top: 12px;
}

@media (max-width: 1000px) {
  .book-mobile-nav {
    display: flex;
  }
}

.mobile-nav-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  padding: 10px 20px;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 1.1rem;
  font-weight: 700;
  transition: background 0.2s, transform 0.15s;
}

.mobile-nav-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.mobile-nav-btn:disabled {
  opacity: 0.2;
  cursor: not-allowed;
}

.mobile-nav-btn:focus-visible {
  outline: 2px solid #fff;
  outline-offset: 2px;
}
</style>
