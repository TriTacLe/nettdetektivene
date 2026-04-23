<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useNotebookStore } from '@/stores/NotebookStore'
import { TaskType } from '@/types'

const route = useRoute()
const notebookStore = useNotebookStore()

const STOP_LABELS: Record<TaskType, string> = {
  [TaskType.NEWS]: 'Nyhetskvartalet',
  [TaskType.PHOTO]: 'Fotografen',
  [TaskType.EMAIL]: 'Postkontoret',
  [TaskType.PASSWORD]: 'Passordbanken',
  [TaskType.MARKETPLACE]: 'Markedsplassen',
  [TaskType.SOCIAL]: 'Sosiale møteplassen',
  [TaskType.BOSS]: 'Datasenteret',
}

const reflections = ref<Record<string, string>>({})
const savingStop = ref<string | null>(null)
const savedStop = ref<string | null>(null)
const currentSpread = ref(0)

const stops = computed(() => Object.values(TaskType))
const totalSpreads = computed(() => Math.ceil(stops.value.length / 2))
const leftStop = computed(() => stops.value[currentSpread.value * 2] ?? null)
const rightStop = computed(() => stops.value[currentSpread.value * 2 + 1] ?? null)

const entriesByStop = computed(() => {
  return notebookStore.entries.reduce<Record<string, typeof notebookStore.entries[0]>>(
    (acc, entry) => {
      acc[entry.stopLocation] = entry
      return acc
    },
    {},
  )
})

function prevSpread(): void {
  if (currentSpread.value > 0) currentSpread.value--
}

function nextSpread(): void {
  if (currentSpread.value < totalSpreads.value - 1) currentSpread.value++
}

function getAutoNotes(stopLocation: string): string[] {
  const entry = entriesByStop.value[stopLocation]
  if (!entry?.autoNotes) return []
  try {
    const parsed = JSON.parse(entry.autoNotes)
    return Array.isArray(parsed) ? parsed : [entry.autoNotes]
  } catch {
    return [entry.autoNotes]
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

async function saveReflection(stopLocation: string): Promise<void> {
  savingStop.value = stopLocation
  savedStop.value = null
  try {
    await notebookStore.saveEntry({
      stopLocation,
      autoNotes: entriesByStop.value[stopLocation]?.autoNotes ?? undefined,
      studentReflection: reflections.value[stopLocation] ?? '',
    })
    savedStop.value = stopLocation
    setTimeout(() => {
      savedStop.value = null
    }, 2000)
  } finally {
    savingStop.value = null
  }
}

onMounted(() => {
  const cid = route.query.classroomId as string | undefined
  if (cid) {
    notebookStore.fetchEntries(cid)
  } else {
    notebookStore.error = 'Mangler klasserom-ID. Naviger hit fra klasserommet ditt.'
  }
})
</script>

<template>
  <SceneComponent title="Dine notater" previous="/student-home" previous-label="Hjem">
    <div class="notebook-wrapper">
      <p v-if="notebookStore.isLoading" class="notebook-status" aria-live="polite">
        Laster inn notatblokk...
      </p>

      <p v-else-if="notebookStore.error" class="notebook-error" role="alert">
        {{ notebookStore.error }}
      </p>

      <template v-else>
        <p v-if="notebookStore.entries.length === 0" class="notebook-empty">
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
              :class="{ 'book-page--locked': !entriesByStop[leftStop] }"
            >
              <div class="card-header">
                <h2 class="stop-title">{{ STOP_LABELS[leftStop] }}</h2>
              </div>

              <div v-if="!entriesByStop[leftStop]" class="locked-content">
                <span class="lock-icon">🔒</span>
                <p class="stop-locked">Fullfør stoppestedet for å få tilgang til notatene dine</p>
              </div>

              <template v-else>
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
              </template>
            </div>

            <div class="book-spine" />

            <div
              v-if="rightStop"
              class="book-page book-page--right"
              :class="{ 'book-page--locked': !entriesByStop[rightStop] }"
            >
              <div class="card-header">
                <h2 class="stop-title">{{ STOP_LABELS[rightStop] }}</h2>
              </div>

              <div v-if="!entriesByStop[rightStop]" class="locked-content">
                <span class="lock-icon">🔒</span>
                <p class="stop-locked">Fullfør stoppestedet for å få tilgang til notatene dine</p>
              </div>

              <template v-else>
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
              </template>
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
