<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import MysteryService from '@/services/MysteryService'
import type { MysteryReviewDTO } from '@/types'

const props = defineProps<{ classroomId: string }>()

const submissions = ref<MysteryReviewDTO[]>([])
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const previewSubmission = ref<MysteryReviewDTO | null>(null)

function openPreview(submission: MysteryReviewDTO) {
  previewSubmission.value = submission
}

function closePreview() {
  previewSubmission.value = null
}

const sortedSubmissions = computed(() =>
  [...submissions.value].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt)),
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

async function approve(submission: MysteryReviewDTO) {
  await review(submission, 'APPROVED', 'Mysteriet er godkjent.')
}

async function reject(submission: MysteryReviewDTO) {
  await review(submission, 'REJECTED', 'Mysteriet er forkastet.')
}

async function review(submission: MysteryReviewDTO, status: 'APPROVED' | 'REJECTED', okMessage: string) {
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const updated = await MysteryService.reviewSubmission(submission.id, { mysStatus: status })
    submissions.value = submissions.value.map((s) => (s.id === updated.id ? updated : s))
    successMessage.value = okMessage
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : 'Kunne ikke oppdatere innsendingen.'
  }
}

function statusText(submission: MysteryReviewDTO) {
  if (submission.mysStatus === 'APPROVED') return 'Godkjent'
  if (submission.mysStatus === 'REJECTED') return 'Forkastet'
  return 'Venter'
}
</script>

<template>
  <div class="mysteries-panel">
    <p v-if="errorMessage" class="message message--error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="message message--success">{{ successMessage }}</p>

    <div class="card-list">
      <p v-if="isLoading" class="empty-state">Laster innsendinger...</p>
      <p v-else-if="sortedSubmissions.length === 0" class="empty-state">
        Ingen innsendinger ennå.
      </p>

      <article
        v-for="(submission, i) in sortedSubmissions"
        v-else
        :key="submission.id"
        class="card"
        :style="{ animationDelay: `${i * 0.05}s` }"
      >
        <header class="card-header">
          <span class="submitter">{{ submission.submittedByName || 'Ukjent elev' }}</span>
          <span class="status-badge" :class="`status-badge--${submission.mysStatus.toLowerCase()}`">
            {{ statusText(submission) }}
          </span>
        </header>

        <div class="card-actions">
          <button
            type="button"
            class="btn-bare action-btn"
            @click="openPreview(submission)"
          >
            Forhåndsvis
          </button>
          <button
            type="button"
            class="btn-bare action-btn"
            :disabled="submission.mysStatus === 'APPROVED'"
            @click="approve(submission)"
          >
            Godkjenn
          </button>
          <button
            type="button"
            class="btn-bare action-btn action-btn-danger"
            :disabled="submission.mysStatus === 'REJECTED'"
            @click="reject(submission)"
          >
            Forkast
          </button>
        </div>
      </article>
    </div>

    <Transition name="fade">
      <div v-if="previewSubmission" class="preview-overlay" @click.self="closePreview">
        <div class="preview-modal">
          <button type="button" class="btn-bare preview-close" aria-label="Lukk forhåndsvisning" @click="closePreview">×</button>
          <img :src="previewSubmission.imagePath" alt="Forhåndsvisning" class="preview-image" />
          <ul class="preview-alternatives">
            <li
              v-for="(text, index) in previewSubmission.alternatives"
              :key="index"
              class="preview-alternative"
              :class="{ 'preview-alternative--correct': index === previewSubmission.correctIndex }"
            >
              <span class="preview-alternative-marker">{{ index === previewSubmission.correctIndex ? '✓' : '·' }}</span>
              <span class="preview-alternative-text">{{ text }}</span>
            </li>
          </ul>
        </div>
      </div>
    </Transition>
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

.card-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.6rem;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0;
  backdrop-filter: blur(4px);
  max-height: calc(100vh - 18rem);
  overflow-y: auto;
}

.card {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 0;
  animation: slideUp 0.4s ease-out both;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.55rem 0.85rem;
  background: #111;
  color: white;
}

.submitter {
  flex: 1;
  font-weight: 700;
  font-size: 0.95rem;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-badge {
  font-size: 0.7rem;
  font-weight: 800;
  border-radius: 0;
  padding: 0.1rem 0.45rem;
  letter-spacing: 0.04em;
}

.status-badge--approved {
  background: #dcfce7;
  color: #166534;
}

.status-badge--rejected {
  background: #fee2e2;
  color: #991b1b;
}

.status-badge--pending {
  background: #fef3c7;
  color: #92400e;
}

.alternatives {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.alternative {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.25rem 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 0;
  background: #fff;
  color: #1f2937;
  font-size: 0.85rem;
  font-weight: 600;
  line-height: 1.3;
}

.alternative--correct {
  border-color: #16a34a;
  background: #f0fdf4;
  color: #166534;
}

.alternative-marker {
  width: 0.85rem;
  text-align: center;
  font-weight: 800;
  color: #16a34a;
  font-size: 0.85rem;
}

.alternative-text {
  flex: 1;
  -webkit-text-stroke: 0;
  text-shadow: none;
}

.card-actions {
  display: flex;
  gap: 0.4rem;
  justify-content: flex-end;
  padding: 0.55rem 0.85rem;
}

.action-btn {
  position: relative;
  isolation: isolate;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  min-height: 1.8rem;
  padding: 0.25rem 0.7rem;
  border: none;
  border-radius: 0 !important;
  clip-path: var(--notch-clip);
  background: transparent;
  font-size: 0.78rem;
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

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

@keyframes slideUp {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

.preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.preview-modal {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  align-items: center;
}

.preview-image {
  max-width: 90vw;
  max-height: 65vh;
  object-fit: contain;
  background: #000;
  border: 2px solid #fff;
}

.preview-alternatives {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  width: min(640px, 90vw);
}

.preview-alternative {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.75rem;
  background: white;
  border: 1px solid #e2e8f0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1f2937;
}

.preview-alternative--correct {
  border-color: #16a34a;
  background: #f0fdf4;
  color: #166534;
}

.preview-alternative-marker {
  width: 1rem;
  text-align: center;
  font-weight: 800;
  color: #16a34a;
}

.preview-alternative-text {
  flex: 1;
}

.preview-close {
  position: absolute;
  top: -2.4rem;
  right: -0.2rem;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  color: #111;
  font-size: 1.5rem;
  font-weight: 800;
  line-height: 1;
  cursor: pointer;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
