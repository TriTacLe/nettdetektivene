<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import MysteryService from '@/services/MysteryService'
import { TaskType, type MysterySubmissionResponseDTO, type MysteryTaskCreateRequestDTO } from '@/types'

const route = useRoute()
const router = useRouter()

const classroomId = computed(() => String(route.params.classroomId ?? route.params.id))

const submissions = ref<MysterySubmissionResponseDTO[]>([])
const selectedSubmission = ref<MysterySubmissionResponseDTO | null>(null)
const isLoading = ref(false)
const isSaving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const taskDraft = ref<MysteryTaskCreateRequestDTO>({
  title: '',
  taskType: TaskType.NEWS,
  stopLocation: 'nyhetskvartalet',
  difficulty: 1,
  content: '',
  isActive: true,
})

const sortedSubmissions = computed(() => [...submissions.value].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt)))
const canCreateTask = computed(() => Boolean(selectedSubmission.value && taskDraft.value.title.trim() && taskDraft.value.taskType && taskDraft.value.difficulty >= 1 && taskDraft.value.difficulty <= 5))

onMounted(loadSubmissions)

async function loadSubmissions() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    submissions.value = await MysteryService.getSubmissionsByClassroom(classroomId.value)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke laste innsendinger.'
  } finally {
    isLoading.value = false
  }
}

function openCreateTask(submission: MysterySubmissionResponseDTO) {
  selectedSubmission.value = submission
  taskDraft.value = {
    title: cleanDescription(submission).slice(0, 80) || 'Ukens mysterium',
    taskType: guessTaskType(submission),
    stopLocation: guessStopLocation(submission),
    difficulty: 1,
    content: makeDefaultContent(submission),
    isActive: true,
  }
  successMessage.value = ''
  errorMessage.value = ''
}

async function approveSubmission(submission: MysterySubmissionResponseDTO, featured = false) {
  try {
    const updated = await MysteryService.reviewSubmission(submission.id, { mysStatus: 'APPROVED', isFeatured: featured })
    replaceSubmission(updated)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke godkjenne innsendingen.'
  }
}

async function rejectSubmission(submission: MysterySubmissionResponseDTO) {
  try {
    const updated = await MysteryService.reviewSubmission(submission.id, { mysStatus: 'REJECTED', isFeatured: false })
    replaceSubmission(updated)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke forkaste innsendingen.'
  }
}

async function createTaskFromSubmission() {
  if (!selectedSubmission.value || !canCreateTask.value) return
  isSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await MysteryService.createTaskFromSubmission(selectedSubmission.value.id, {
      ...taskDraft.value,
      title: taskDraft.value.title.trim(),
      stopLocation: taskDraft.value.stopLocation?.trim() || null,
      content: taskDraft.value.content?.trim() || null,
    })
    const refreshed = await MysteryService.getSubmissionsByClassroom(classroomId.value)
    submissions.value = refreshed
    selectedSubmission.value = null
    successMessage.value = 'Oppgave opprettet og lagt til i klasserommet.'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke lage oppgave av innsendingen.'
  } finally {
    isSaving.value = false
  }
}

function replaceSubmission(updated: MysterySubmissionResponseDTO) {
  submissions.value = submissions.value.map((submission) => submission.id === updated.id ? updated : submission)
}

function categoryFromSubmission(submission: MysterySubmissionResponseDTO) {
  return submission.mysDesc.match(/^\[(.*?)]/)?.[1] ?? 'Falsk nyhet'
}

function cleanDescription(submission: MysterySubmissionResponseDTO) {
  return submission.mysDesc.replace(/^\[.*?]\s*/, '')
}

function guessTaskType(submission: MysterySubmissionResponseDTO) {
  const category = categoryFromSubmission(submission).toLowerCase()
  if (category.includes('bilde')) return TaskType.PHOTO
  if (category.includes('nettside')) return TaskType.NEWS
  return TaskType.NEWS
}

function guessStopLocation(submission: MysterySubmissionResponseDTO) {
  return guessTaskType(submission) === TaskType.PHOTO ? 'fotografen' : 'nyhetskvartalet'
}

function makeDefaultContent(submission: MysterySubmissionResponseDTO) {
  return JSON.stringify({
    description: cleanDescription(submission),
    sourceUrl: submission.attachmentUrl,
    mysterySubmissionId: submission.id,
    weekNumber: submission.weekNumber,
    weekYear: submission.weekYear,
  }, null, 2)
}

function statusText(submission: MysterySubmissionResponseDTO) {
  if (submission.mysStatus === 'APPROVED') return submission.isFeatured ? 'Godkjent og fremhevet' : 'Godkjent'
  if (submission.mysStatus === 'REJECTED') return 'Forkastet'
  return 'Venter'
}
</script>


<template>
  <SceneComponent
    title="Ukens mysterium"
    background="/StartBackground.jpg"
    @previous="router.back()"
  >
    <div class="flex justify-center p-4 flex-1 overflow-y-auto">
      <div class="w-full max-w-6xl flex flex-col gap-4">
        <header class="page-header">
          <div>
            <p>Ukens mysterium</p>
            <h1>Innsendinger fra elever</h1>
          </div>
          <button class="secondary-button" type="button" @click="loadSubmissions">
            Oppdater
          </button>
        </header>

        <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
        <p v-if="successMessage" class="message success">{{ successMessage }}</p>

        <section class="layout">
          <div class="submission-panel card">
            <div v-if="isLoading" class="empty-state">Laster innsendinger...</div>
            <div v-else-if="sortedSubmissions.length === 0" class="empty-state">
              Ingen innsendinger ennå.
            </div>

            <article
              v-for="submission in sortedSubmissions"
              v-else
              :key="submission.id"
              class="submission-card"
            >
              <div class="submission-topline">
                <span>Uke {{ submission.weekNumber }}</span>
                <span class="status-chip" :class="submission.mysStatus.toLowerCase()">
                  {{ statusText(submission) }}
                </span>
              </div>

              <h2>{{ cleanDescription(submission) }}</h2>
              <p class="category">{{ categoryFromSubmission(submission) }}</p>

              <a
                v-if="submission.attachmentUrl"
                :href="submission.attachmentUrl"
                target="_blank"
                rel="noreferrer"
              >
                Åpne elevens lenke
              </a>

              <div class="actions">
                <button
                  type="button"
                  class="secondary-button"
                  @click="approveSubmission(submission, true)"
                >
                  Godkjenn og fremhev
                </button>
                <button
                  type="button"
                  class="secondary-button danger"
                  @click="rejectSubmission(submission)"
                >
                  Forkast
                </button>
                <button
                  type="button"
                  class="primary-button"
                  @click="openCreateTask(submission)"
                >
                  Lag oppgave
                </button>
              </div>
            </article>
          </div>

          <form class="task-panel card" @submit.prevent="createTaskFromSubmission">
            <h2>Lag oppgave av innsending</h2>

            <p v-if="!selectedSubmission" class="empty-state compact">
              Velg “Lag oppgave” på en innsending.
            </p>

            <template v-else>
              <label>
                Tittel
                <input v-model="taskDraft.title" required />
              </label>

              <label>
                Type
                <select v-model="taskDraft.taskType">
                  <option
                    v-for="type in Object.values(TaskType)"
                    :key="type"
                    :value="type"
                  >
                    {{ type }}
                  </option>
                </select>
              </label>

              <label>
                Stop location
                <input v-model="taskDraft.stopLocation" placeholder="nyhetskvartalet" />
              </label>

              <label>
                Vanskelighetsgrad
                <input v-model.number="taskDraft.difficulty" min="1" max="5" type="number" />
              </label>

              <label class="checkbox-label">
                <input v-model="taskDraft.isActive" type="checkbox" />
                Aktiv i klasserommet
              </label>

              <label>
                Oppgaveinnhold JSON
                <textarea v-model="taskDraft.content" rows="10" />
              </label>

              <div class="actions end">
                <button type="button" class="secondary-button" @click="selectedSubmission = null">
                  Avbryt
                </button>
                <button
                  type="submit"
                  class="primary-button"
                  :disabled="!canCreateTask || isSaving"
                >
                  {{ isSaving ? 'Lagrer...' : 'Opprett oppgave' }}
                </button>
              </div>
            </template>
          </form>
        </section>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  color: white;
}

.page-header p {
  margin: 0 0 .25rem;
  color: rgba(255,255,255,.75);
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: .04em;
}

.page-header h1 {
  margin: 0;
  font-size: 2rem;
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(360px, .75fr);
  gap: 1.25rem;
}

.card {
  background: rgba(255,255,255,.94);
  border-radius: 16px;
  box-shadow: 0 14px 30px rgba(15,23,42,.18);
  padding: 1.25rem;
}

.submission-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.submission-card {
  border: 1px solid #dce2ea;
  border-radius: 14px;
  padding: 1rem;
  background: #fbfcff;
}

.submission-topline {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  color: #64748b;
  font-size: .85rem;
  font-weight: 800;
}

.submission-card h2 {
  margin: .7rem 0 .35rem;
  font-size: 1.1rem;
}

.category {
  margin: 0 0 .8rem;
  color: #475569;
  font-weight: 800;
}

.submission-card a {
  color: #0f5c75;
  font-weight: 800;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: .6rem;
  margin-top: 1rem;
}

.actions.end {
  justify-content: flex-end;
}

.primary-button,
.secondary-button {
  border: 0;
  border-radius: 10px;
  padding: .75rem 1rem;
  font-weight: 900;
  cursor: pointer;
}

.primary-button {
  background: #132a34;
  color: white;
}

.primary-button:disabled {
  opacity: .55;
  cursor: not-allowed;
}

.secondary-button {
  background: #e9eef5;
  color: #132a34;
}

.secondary-button.danger {
  background: #fee2e2;
  color: #b91c1c;
}

.status-chip {
  border-radius: 999px;
  padding: .35rem .7rem;
  font-size: .78rem;
  font-weight: 900;
}

.pending {
  background: #fff7ed;
  color: #d97706;
}

.approved {
  background: #ecfdf3;
  color: #15803d;
}

.rejected {
  background: #fef2f2;
  color: #dc2626;
}

.task-panel {
  display: flex;
  flex-direction: column;
  gap: .85rem;
  align-self: start;
  position: sticky;
  top: 1rem;
}

.task-panel h2 {
  margin: 0 0 .5rem;
}

.task-panel label {
  display: flex;
  flex-direction: column;
  gap: .35rem;
  font-size: .85rem;
  font-weight: 900;
  color: #334155;
}

.task-panel input,
.task-panel select,
.task-panel textarea {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: .75rem;
  font: inherit;
  color: #10212a;
  background: #f8fafc;
}

.task-panel textarea {
  resize: vertical;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: .82rem;
}

.checkbox-label {
  flex-direction: row !important;
  align-items: center;
}

.message {
  font-weight: 900;
  border-radius: 12px;
  padding: .8rem 1rem;
  background: white;
}

.error {
  color: #dc2626;
}

.success {
  color: #15803d;
}

.empty-state {
  padding: 2rem;
  text-align: center;
  color: #64748b;
  font-weight: 800;
}

.empty-state.compact {
  padding: 1rem;
  background: #f8fafc;
  border-radius: 12px;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .task-panel {
    position: static;
  }
}
</style>
