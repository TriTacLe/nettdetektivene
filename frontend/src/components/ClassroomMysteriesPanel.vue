<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import MysteryService from '@/services/MysteryService'
import { TaskType, type MysterySubmissionResponseDTO, type MysteryTaskCreateRequestDTO } from '@/types'

const props = defineProps<{ classroomId: string }>()

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

const sortedSubmissions = computed(() =>
  [...submissions.value].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt)),
)
const canCreateTask = computed(
  () =>
    Boolean(
      selectedSubmission.value &&
        taskDraft.value.title.trim() &&
        taskDraft.value.taskType &&
        taskDraft.value.difficulty >= 1 &&
        taskDraft.value.difficulty <= 5,
    ),
)

onMounted(loadSubmissions)
watch(() => props.classroomId, loadSubmissions)

async function loadSubmissions() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    submissions.value = await MysteryService.getSubmissionsByClassroom(props.classroomId)
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
    const updated = await MysteryService.reviewSubmission(submission.id, {
      mysStatus: 'APPROVED',
      isFeatured: featured,
    })
    replaceSubmission(updated)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke godkjenne innsendingen.'
  }
}

async function rejectSubmission(submission: MysterySubmissionResponseDTO) {
  try {
    const updated = await MysteryService.reviewSubmission(submission.id, {
      mysStatus: 'REJECTED',
      isFeatured: false,
    })
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
    submissions.value = await MysteryService.getSubmissionsByClassroom(props.classroomId)
    selectedSubmission.value = null
    successMessage.value = 'Oppgave opprettet og lagt til i klasserommet.'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke lage oppgave av innsendingen.'
  } finally {
    isSaving.value = false
  }
}

function replaceSubmission(updated: MysterySubmissionResponseDTO) {
  submissions.value = submissions.value.map((submission) =>
    submission.id === updated.id ? updated : submission,
  )
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
  return TaskType.NEWS
}

function guessStopLocation(submission: MysterySubmissionResponseDTO) {
  return guessTaskType(submission) === TaskType.PHOTO ? 'fotografen' : 'nyhetskvartalet'
}

function makeDefaultContent(submission: MysterySubmissionResponseDTO) {
  return JSON.stringify(
    {
      description: cleanDescription(submission),
      sourceUrl: submission.attachmentUrl,
      mysterySubmissionId: submission.id,
      weekNumber: submission.weekNumber,
      weekYear: submission.weekYear,
    },
    null,
    2,
  )
}

function statusText(submission: MysterySubmissionResponseDTO) {
  if (submission.mysStatus === 'APPROVED') {
    return submission.isFeatured ? 'Godkjent og fremhevet' : 'Godkjent'
  }
  if (submission.mysStatus === 'REJECTED') return 'Forkastet'
  return 'Venter'
}
</script>

<template>
  <div class="mysteries-panel">
    <p v-if="errorMessage" class="message message--error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="message message--success">{{ successMessage }}</p>

    <div class="row-list">
      <p v-if="isLoading" class="empty-state">Laster innsendinger...</p>
      <p v-else-if="sortedSubmissions.length === 0" class="empty-state">
        Ingen innsendinger ennå.
      </p>

      <article
        v-for="(submission, i) in sortedSubmissions"
        v-else
        :key="submission.id"
        class="row"
        :style="{ animationDelay: `${i * 0.05}s` }"
      >
        <span class="row-week">Uke {{ submission.weekNumber }}</span>
        <span class="row-title" :title="cleanDescription(submission)">
          {{ cleanDescription(submission) }}
        </span>
        <span class="row-stat">{{ categoryFromSubmission(submission) }}</span>
        <a
          v-if="submission.attachmentUrl"
          :href="submission.attachmentUrl"
          target="_blank"
          rel="noreferrer"
          class="row-link"
        >Lenke</a>
        <span class="row-badge" :class="`badge-${submission.mysStatus.toLowerCase()}`">
          {{ statusText(submission) }}
        </span>

        <div class="row-actions">
          <button type="button" class="btn-bare action-btn" @click="approveSubmission(submission, true)">
            Godkjenn
          </button>
          <button type="button" class="btn-bare action-btn action-btn-danger" @click="rejectSubmission(submission)">
            Forkast
          </button>
          <button type="button" class="btn-bare action-btn" @click="openCreateTask(submission)">
            Lag oppgave
          </button>
        </div>
      </article>
    </div>

    <form v-if="selectedSubmission" class="task-form" @submit.prevent="createTaskFromSubmission">
      <h3 class="form-title">Lag oppgave av innsending</h3>

      <label class="field">
        <span>Tittel</span>
        <input v-model="taskDraft.title" required />
      </label>

      <label class="field">
        <span>Type</span>
        <select v-model="taskDraft.taskType">
          <option v-for="type in Object.values(TaskType)" :key="type" :value="type">
            {{ type }}
          </option>
        </select>
      </label>

      <label class="field">
        <span>Stop location</span>
        <input v-model="taskDraft.stopLocation" placeholder="nyhetskvartalet" />
      </label>

      <label class="field">
        <span>Vanskelighetsgrad (1–5)</span>
        <input v-model.number="taskDraft.difficulty" min="1" max="5" type="number" />
      </label>

      <label class="checkbox-field">
        <input v-model="taskDraft.isActive" type="checkbox" />
        <span>Aktiv i klasserommet</span>
      </label>

      <label class="field">
        <span>Oppgaveinnhold (JSON)</span>
        <textarea v-model="taskDraft.content" rows="10" />
      </label>

      <div class="actions actions--end">
        <button type="button" class="btn-vn btn-vn-ghost" @click="selectedSubmission = null">
          Avbryt
        </button>
        <button type="submit" class="btn-vn" :disabled="!canCreateTask || isSaving">
          {{ isSaving ? 'Lagrer...' : 'Opprett oppgave' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.mysteries-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  width: 100%;
  margin-top: 0.5rem;
}

.row-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0.875rem;
  backdrop-filter: blur(4px);
  max-height: calc(100vh - 18rem);
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.row-list::-webkit-scrollbar {
  width: 8px;
}
.row-list::-webkit-scrollbar-track {
  background: transparent;
}
.row-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.18);
  border-radius: 4px;
}
.row-list::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 1.5rem;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  clip-path: var(--notch-clip);
  animation: slideUp 0.4s ease-out both;
}

.row-week {
  font-size: 0.85rem;
  font-weight: 800;
  color: #64748b;
  white-space: nowrap;
  flex-shrink: 0;
}

.row-title {
  font-size: 1rem;
  font-weight: 800;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 6rem;
  -webkit-text-stroke: 0;
  text-shadow: none;
}

.row-stat {
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
  white-space: nowrap;
  flex-shrink: 0;
}

.row-link {
  color: #1d4ed8;
  font-weight: 700;
  font-size: 0.85rem;
  white-space: nowrap;
  flex-shrink: 0;
}

.row-badge {
  font-size: 0.8rem;
  font-weight: 700;
  border-radius: 9999px;
  padding: 0.25rem 0.65rem;
  white-space: nowrap;
  flex-shrink: 0;
}

.badge-approved {
  background: #dcfce7;
  color: #166534;
}

.badge-rejected {
  background: #fee2e2;
  color: #991b1b;
}

.badge-pending {
  background: #fef3c7;
  color: #92400e;
}

.row-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
  margin-left: auto;
}

.action-btn {
  position: relative;
  isolation: isolate;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  min-height: 2.5rem;
  padding: 0.45rem 0.95rem;
  border: none;
  border-radius: 0 !important;
  clip-path: var(--notch-clip);
  background: transparent;
  font-size: 0.875rem;
  font-weight: 700;
  color: #1f2937;
  cursor: pointer;
}

.action-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: #d1d5db;
  z-index: -2;
}

.action-btn::after {
  content: '';
  position: absolute;
  inset: 1px;
  clip-path: var(--notch-clip);
  background: white;
  z-index: -1;
  transition: background 0.15s;
}

.action-btn:hover:not(:disabled)::after {
  background: #f9fafb;
}

.action-btn:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

.action-btn-danger {
  color: #b91c1c;
}

.action-btn-danger::before {
  background: #fca5a5;
}

.action-btn-danger:hover:not(:disabled)::after {
  background: #fef2f2;
}

.empty-state {
  color: rgba(255, 255, 255, 0.85);
  text-align: center;
  padding: 1.5rem;
  margin: 0;
  font-weight: 700;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

.actions--end {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
  margin-top: 0.5rem;
  flex-wrap: wrap;
}

.task-form {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 1rem;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.18);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  color: #0f172a;
}

.form-title {
  margin: 0 0 0.25rem;
  color: #0f172a;
  font-size: 1.1rem;
  font-weight: 800;
  -webkit-text-stroke: 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.85rem;
  font-weight: 700;
}

.field span {
  color: #1f2937;
}

.field select,
.field textarea {
  background: #fff;
  color: #000;
  border: 2px solid #000;
  border-radius: 0.5rem;
  padding: 0.75rem 1rem;
  font: inherit;
  font-size: 1rem;
  font-weight: 500;
}

.field textarea {
  resize: vertical;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.85rem;
}

.checkbox-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  color: #1f2937;
}

.checkbox-field input {
  width: 1.1rem;
  height: 1.1rem;
  padding: 0;
  border-width: 2px;
  cursor: pointer;
}

.message {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
  padding: 0.6rem 0.85rem;
  border-radius: 0.5rem;
}

.message--error {
  color: #991b1b;
  background: #fee2e2;
  border: 1px solid #fca5a5;
}

.message--success {
  color: #15803d;
  background: #dcfce7;
  border: 1px solid #86efac;
}
</style>
