<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import ClassroomService from '@/services/ClassroomService'
import MysteryService from '@/services/MysteryService'
import { useStudentStore } from '@/stores/StudentStore'
import type { MysteryStatus, MysterySubmissionResponseDTO } from '@/types'

const route = useRoute()
const router = useRouter()
const studentStore = useStudentStore()

const categories = ['Falsk nyhet', 'Manipulert bilde', 'Mistenkelig nettside']
const routeClassroomId = typeof route.params.classroomId === 'string' ? route.params.classroomId : null
const classroomId = ref<string | null>(routeClassroomId ?? studentStore.classroomId)
const selectedCategory = ref(categories[0])
const exampleUrl = ref('')
const description = ref('')
const submissions = ref<MysterySubmissionResponseDTO[]>([])
const featuredSubmission = ref<MysterySubmissionResponseDTO | null>(null)
const isLoading = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const previousPath = computed(() =>
  classroomId.value ? `/student/classrooms/${classroomId.value}/mystery` : '/student-home',
)

const currentWeek = computed(
  () => submissions.value[0]?.weekNumber ?? featuredSubmission.value?.weekNumber ?? getIsoWeek(new Date()),
)
const weeklyPrompt = computed(
  () => featuredSubmission.value?.mysDesc || 'Har du sett en falsk nyhet på nettet denne uken?',
)
const visibleSubmissions = computed(() =>
  [...submissions.value]
    .sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt))
    .slice(0, 4),
)
const canSubmit = computed(
  () => Boolean(classroomId.value && selectedCategory.value && description.value.trim()),
)

onMounted(loadMysteryData)

function goBack() {
  if (classroomId.value) {
    router.push({ name: 'student-weekly-mystery', params: { classroomId: classroomId.value } })
  } else {
    router.push('/student-home')
  }
}

async function loadMysteryData() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    if (!classroomId.value) {
      const classrooms = await ClassroomService.getMyClassrooms()
      classroomId.value = classrooms[0]?.id ?? null
    }
    if (!classroomId.value) {
      errorMessage.value = 'Fant ikke klasserommet ditt.'
      return
    }
    const [mine, featured] = await Promise.all([
      MysteryService.getMySubmissions(classroomId.value),
      MysteryService.getFeaturedSubmission(classroomId.value),
    ])
    submissions.value = mine
    featuredSubmission.value = featured
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke laste Ukens Mysterium.'
  } finally {
    isLoading.value = false
  }
}

async function submitMystery() {
  if (!classroomId.value || !canSubmit.value) return
  isSubmitting.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const created = await MysteryService.submitMystery({
      classroomId: classroomId.value,
      mysDesc: `[${selectedCategory.value}] ${description.value.trim()}`,
      attachmentUrl: exampleUrl.value.trim() || null,
    })
    submissions.value = [created, ...submissions.value]
    exampleUrl.value = ''
    description.value = ''
    successMessage.value = 'Eksempelet er sendt til læreren.'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke sende inn eksempelet.'
  } finally {
    isSubmitting.value = false
  }
}

function statusText(status: MysteryStatus) {
  if (status === 'APPROVED') return 'Godkjent'
  if (status === 'REJECTED') return 'Forkastet'
  return 'Venter'
}

function categoryFromSubmission(submission: MysterySubmissionResponseDTO) {
  return submission.mysDesc.match(/^\[(.*?)]/)?.[1] ?? 'Falsk nyhet'
}

function cleanDescription(submission: MysterySubmissionResponseDTO) {
  return submission.mysDesc.replace(/^\[.*?]\s*/, '')
}

function getIsoWeek(date: Date) {
  const target = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const dayNumber = target.getUTCDay() || 7
  target.setUTCDate(target.getUTCDate() + 4 - dayNumber)
  const yearStart = new Date(Date.UTC(target.getUTCFullYear(), 0, 1))
  return Math.ceil(((target.getTime() - yearStart.getTime()) / 86400000 + 1) / 7)
}
</script>

<template>
  <SceneComponent
    title="Ukens Mysterium"
    :previous="previousPath"
    background="/StartBackground.jpg"
    @back="goBack"
  >
    <main class="flex flex-1 justify-center p-6">
      <div class="w-full max-w-3xl flex flex-col gap-4">
        <section class="panel">
          <p class="eyebrow">Ukens Mysterium · Uke {{ currentWeek }}</p>
          <h2 class="panel-title">{{ weeklyPrompt }}</h2>
          <p class="panel-body">
            Send inn et eksempel du har funnet på nettet — en falsk nyhet, et manipulert bilde
            eller en mistenkelig nettside. Læreren godkjenner de beste bidragene.
          </p>
        </section>

        <form class="panel" @submit.prevent="submitMystery">
          <h2 class="panel-title">Send inn ditt eksempel</h2>

          <fieldset class="category-group">
            <legend class="field-label">Hva slags eksempel er dette?</legend>
            <label
              v-for="category in categories"
              :key="category"
              class="category-option"
              :class="{ 'category-option--active': selectedCategory === category }"
            >
              <input v-model="selectedCategory" type="radio" :value="category" class="sr-only" />
              <span>{{ category }}</span>
            </label>
          </fieldset>

          <label class="field-label" for="example-url">Lenke til eksempelet (URL)</label>
          <input id="example-url" v-model="exampleUrl" type="url" placeholder="https://..." />

          <label class="field-label" for="description">Beskriv kort hva som er mistenkelig</label>
          <textarea
            id="description"
            v-model="description"
            class="text-area"
            rows="4"
            maxlength="500"
            placeholder="F.eks. «Overskriften er veldig dramatisk og kilden er ukjent...»"
            required
          />

          <p v-if="errorMessage" class="message message--error">{{ errorMessage }}</p>
          <p v-if="successMessage" class="message message--success">{{ successMessage }}</p>

          <div class="form-actions">
            <button type="submit" class="btn-vn" :disabled="!canSubmit || isSubmitting">
              {{ isSubmitting ? 'Sender...' : 'Send inn eksempel' }}
            </button>
          </div>
        </form>

        <section class="panel">
          <h2 class="panel-title">Mine innsendte eksempler</h2>
          <p v-if="isLoading" class="panel-body">Laster...</p>
          <p v-else-if="visibleSubmissions.length === 0" class="panel-body">
            Du har ikke sendt inn noen eksempler ennå.
          </p>
          <ul v-else class="submission-list">
            <li v-for="submission in visibleSubmissions" :key="submission.id" class="submission-item">
              <div class="submission-meta">
                <span class="submission-week">Uke {{ submission.weekNumber }}</span>
                <span class="submission-category">{{ categoryFromSubmission(submission) }}</span>
                <span class="status-chip" :class="`status-chip--${submission.mysStatus.toLowerCase()}`">
                  {{ statusText(submission.mysStatus) }}
                </span>
              </div>
              <p class="submission-desc">{{ cleanDescription(submission) }}</p>
              <a
                v-if="submission.attachmentUrl"
                :href="submission.attachmentUrl"
                target="_blank"
                rel="noreferrer"
                class="submission-link"
              >
                Åpne lenke
              </a>
            </li>
          </ul>
        </section>
      </div>
    </main>
  </SceneComponent>
</template>

<style scoped>
.panel {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 1rem;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.18);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.eyebrow {
  margin: 0;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.78rem;
  font-weight: 800;
}

.panel-title {
  margin: 0;
  color: #0f172a;
  font-size: 1.2rem;
  font-weight: 800;
  -webkit-text-stroke: 0;
}

.panel-body {
  margin: 0;
  color: #475569;
  font-weight: 500;
  line-height: 1.5;
}

.field-label {
  color: #1f2937;
  font-size: 0.85rem;
  font-weight: 700;
  margin-top: 0.25rem;
}

.category-group {
  border: 0;
  padding: 0;
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.category-option {
  display: inline-flex;
  align-items: center;
  border: 2px solid #111;
  border-radius: 0.5rem;
  padding: 0.5rem 0.85rem;
  background: #fff;
  color: #1f2937;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.category-option--active {
  background: #111;
  color: #fff;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.text-area {
  width: 100%;
  background: #fff;
  color: #000;
  border: 2px solid #000;
  border-radius: 0.5rem;
  padding: 0.75rem 1rem;
  font: inherit;
  font-size: 1rem;
  font-weight: 500;
  resize: vertical;
  min-height: 6rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
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

.submission-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.submission-item {
  border: 1px solid #d7dce7;
  border-radius: 0.75rem;
  background: #f8f8fc;
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.submission-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  font-size: 0.78rem;
  font-weight: 700;
}

.submission-week {
  color: #64748b;
}

.submission-category {
  background: #e7e6f5;
  color: #475569;
  border-radius: 0.4rem;
  padding: 0.2rem 0.5rem;
}

.submission-desc {
  margin: 0;
  color: #0f172a;
  font-weight: 700;
  font-size: 0.95rem;
}

.submission-link {
  color: #1d4ed8;
  font-weight: 700;
  font-size: 0.85rem;
  align-self: start;
}

.status-chip {
  margin-left: auto;
  border-radius: 0.4rem;
  padding: 0.2rem 0.55rem;
  font-size: 0.75rem;
  font-weight: 800;
}

.status-chip--approved {
  color: #166534;
  background: #dcfce7;
}

.status-chip--rejected {
  color: #991b1b;
  background: #fee2e2;
}

.status-chip--pending {
  color: #92400e;
  background: #fef3c7;
}
</style>
