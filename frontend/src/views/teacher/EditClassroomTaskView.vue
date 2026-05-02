<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { getClassroomOptions } from '@/generated-client/@tanstack/vue-query.gen'
import ClassroomService from '@/services/ClassroomService'
import { updateTeacherTaskFull } from '@/services/TaskService'
import { TaskType, type EditNewsTaskDraft, type EditPhotoTaskDraft, type EditTaskDraft, type NewsChallengeContent, type PhotoChallengeContent } from '@/types'
import { parseTaskContent } from '@/services/TaskService'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const classroomId = route.params.id as string
const taskId = route.params.taskId as string

const { data: classroomData } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroom = computed(() => classroomData.value?.data ?? null)

const tasksQuery = useQuery({
  queryKey: ['teacher-classroom-tasks', classroomId],
  queryFn: () => ClassroomService.getClassroomTasks(classroomId, false, false),
})

const classroomTask = computed(() => tasksQuery.data.value?.find(t => t.taskId === taskId))

const draft = ref<EditTaskDraft | null>(null)
const serverError = ref<string | null>(null)
const initialized = ref(false)

watch(classroomTask, (task) => {
  if (!task || initialized.value) return
  initialized.value = true
  const t = task.task

  const parsed = parseTaskContent(t.content as string, t.taskType)

  if (t.taskType === TaskType.NEWS) {
    const wrapper = parsed as { news?: NewsChallengeContent } | null
    const content = wrapper?.news ?? (parsed as NewsChallengeContent | null)
    const newsDraft: EditNewsTaskDraft = {
      type: TaskType.NEWS,
      title: t.title,
      difficulty: t.difficulty,
      stopLocation: t.stopLocation ?? 'nyhetskvartalet',
      question: content?.question ?? '',
      hint: content?.hint ?? '',
      articles: (content?.articles ?? []).map((a) => ({
        existingImagePath: a.imagePath ?? null,
        image: null,
        isReal: a.isReal,
      })),
    }
    draft.value = newsDraft
  } else if (t.taskType === TaskType.PHOTO) {
    const wrapper = parsed as { photo?: PhotoChallengeContent } | null
    const content = wrapper?.photo ?? (parsed as PhotoChallengeContent | null)
    const photoDraft: EditPhotoTaskDraft = {
      type: TaskType.PHOTO,
      title: t.title,
      difficulty: t.difficulty,
      stopLocation: t.stopLocation ?? 'fotografen',
      description: content?.description ?? '',
      hint: content?.hint ?? '',
      alternatives: (content?.alternatives ?? []).map((a) => ({
        existingImagePath: a.imagePath ?? null,
        image: null,
        correctType: a.correctType ?? 'ekte',
      })),
    }
    draft.value = photoDraft
  }
}, { immediate: true })

const saveMutation = useMutation({
  mutationFn: async () => {
    if (!draft.value) throw new Error('Ingen data')
    validate()
    await updateTeacherTaskFull(taskId, draft.value)
    await ClassroomService.updateClassroomTask(classroomId, taskId, {
      isActive: classroomTask.value?.isActive ?? true,
      sortOrder: classroomTask.value?.sortOrder ?? 0,
    })
  },
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['teacher-classroom-tasks', classroomId] })
    router.push({ name: 'classroom-detail', params: { id: classroomId } })
  },
  onError: (error: Error) => {
    serverError.value = error.message || 'Kunne ikke oppdatere oppgaven'
  },
})

const typeDefaultStop = computed(() =>
  draft.value?.type === TaskType.NEWS ? 'nyhetskvartalet' : 'fotografen'
)

function selectDifficulty(n: number) {
  if (!draft.value) return
  draft.value.difficulty = n
  draft.value.stopLocation = typeDefaultStop.value
}

function selectDatasenteret() {
  if (!draft.value) return
  draft.value.difficulty = 4
  draft.value.stopLocation = 'datasenteret'
}

function validate() {
  if (!draft.value) throw new Error('Ingen data')
  if (!draft.value.title.trim()) throw new Error('Oppgaven må ha en tittel')

  if (draft.value.type === TaskType.NEWS) {
    if (!draft.value.question.trim()) throw new Error('Oppgaven må ha et spørsmål')
    for (const article of draft.value.articles) {
      if (!article.image && !article.existingImagePath) throw new Error('Alle artikler må ha et bilde')
    }
    const hasReal = draft.value.articles.some(a => a.isReal)
    const hasFake = draft.value.articles.some(a => !a.isReal)
    if (!hasReal || !hasFake) throw new Error('Du må ha én ekte og én falsk artikkel')
  }

  if (draft.value.type === TaskType.PHOTO) {
    if (!draft.value.description.trim()) throw new Error('Oppgaven må ha en beskrivelse')
    for (const alt of draft.value.alternatives) {
      if (!alt.image && !alt.existingImagePath) throw new Error('Alle alternativer må ha et bilde')
    }
  }
}

const previewUrls = new WeakMap<File, string>()

function previewUrl(file: File | null): string | null {
  if (!file) return null
  if (!previewUrls.has(file)) previewUrls.set(file, URL.createObjectURL(file))
  return previewUrls.get(file)!
}

function handleImageChange(target: { image: File | null }, event: Event) {
  const input = event.target as HTMLInputElement
  target.image = input.files?.[0] ?? null
}

function addAlternative() {
  if (draft.value?.type !== TaskType.PHOTO) return
  if (draft.value.alternatives.length >= 4) {
    serverError.value = 'Maksimalt 4 bildealternativer tillatt'
    return
  }
  serverError.value = null
  draft.value.alternatives.push({ existingImagePath: null, image: null, correctType: 'ekte' })
}

function removeAlternative(index: number) {
  if (draft.value?.type !== TaskType.PHOTO) return
  if (draft.value.alternatives.length <= 2) return
  draft.value.alternatives.splice(index, 1)
}

function submit() {
  serverError.value = null
  saveMutation.mutate()
}
</script>

<template>
  <SceneComponent
    :title="classroom?.title ? `Rediger oppgave: ${classroom.title}` : 'Rediger oppgave'"
    :previous="`/classroom/${classroomId}`"
    background="/corkboard.jpg"
  >
    <div class="task-create-page">
      <div v-if="tasksQuery.isLoading.value" class="task-state">Laster...</div>
      <div v-else-if="!classroomTask" class="task-state task-state--error">Oppgave ikke funnet.</div>

      <form v-else-if="draft" class="task-create-panel task-create-form" :class="{ 'task-create-panel--photo': draft.type === TaskType.PHOTO }" @submit.prevent="submit">
        <div v-if="serverError" class="task-create-error">{{ serverError }}</div>

        <!-- NEWS form -->
        <template v-if="draft.type === TaskType.NEWS">
          <div class="task-template task-template--news">
            <div class="task-template__hero">
              <div class="task-field">
                <span class="task-field-label">Vanskelighetsgrad</span>
                <div class="diff-selector">
                  <button v-for="n in 3" :key="n" type="button"
                    :class="['btn-bare diff-btn', { 'diff-btn--active': draft.difficulty === n && draft.stopLocation !== 'datasenteret' }]"
                    @click="selectDifficulty(n)">
                    <span v-for="i in n" :key="i" class="diff-star">★</span>
                  </button>
                  <button type="button"
                    :class="['btn-bare diff-btn diff-btn--ds', { 'diff-btn--active': draft.stopLocation === 'datasenteret' }]"
                    @click="selectDatasenteret()">
                    Datasenteret
                  </button>
                </div>
              </div>
              <label class="task-field">
                <span>Tittel</span>
                <input v-model="draft.title" type="text" />
              </label>
              <label class="task-field">
                <span>Spørsmål</span>
                <textarea v-model="draft.question" rows="2" />
              </label>
            </div>

            <div class="task-card-grid task-card-grid--news">
              <article
                v-for="(article, index) in draft.articles"
                :key="index"
                class="task-upload-card task-upload-card--news"
                :class="article.isReal ? 'task-upload-card--good' : 'task-upload-card--fake'"
              >
                <div class="task-upload-box">
                  <img v-if="previewUrl(article.image) || article.existingImagePath"
                    :src="previewUrl(article.image) ?? article.existingImagePath!"
                    class="task-existing-preview" alt="Forhåndsvisning" />
                  <strong>{{ article.image ? article.image.name : (article.existingImagePath ? 'Klikk for å erstatte bilde' : 'Klikk for å laste opp') }}</strong>
                  <p>{{ article.image ? 'Ny fil valgt' : (article.existingImagePath ? 'Eksisterende bilde' : 'Last opp bilde av artikkel') }}</p>
                  <input type="file" accept="image/*" @change="handleImageChange(article, $event)" />
                </div>
                <div class="task-card-toggle">
                  <label class="task-check">
                    <input v-model="article.isReal" :value="true" type="radio" :name="`article-real-${index}`" />
                    <span>Ekte</span>
                  </label>
                  <label class="task-check">
                    <input v-model="article.isReal" :value="false" type="radio" :name="`article-real-${index}`" />
                    <span>Falsk</span>
                  </label>
                </div>
              </article>
            </div>
          </div>
        </template>

        <!-- PHOTO form -->
        <template v-else-if="draft.type === TaskType.PHOTO">
          <div class="task-template task-template--photo">
            <div class="task-template__hero">
              <div class="task-field">
                <span class="task-field-label">Vanskelighetsgrad</span>
                <div class="diff-selector">
                  <button v-for="n in 3" :key="n" type="button"
                    :class="['btn-bare diff-btn', { 'diff-btn--active': draft.difficulty === n && draft.stopLocation !== 'datasenteret' }]"
                    @click="selectDifficulty(n)">
                    <span v-for="i in n" :key="i" class="diff-star">★</span>
                  </button>
                  <button type="button"
                    :class="['btn-bare diff-btn diff-btn--ds', { 'diff-btn--active': draft.stopLocation === 'datasenteret' }]"
                    @click="selectDatasenteret()">
                    Datasenteret
                  </button>
                </div>
              </div>
              <label class="task-field">
                <span>Tittel</span>
                <input v-model="draft.title" type="text" />
              </label>
              <label class="task-field">
                <span>Oppgavetekst</span>
                <textarea v-model="draft.description" rows="2" />
              </label>
            </div>

            <div class="task-card-grid task-card-grid--photo">
              <article
                v-for="(alt, index) in draft.alternatives"
                :key="index"
                class="task-upload-card task-upload-card--photo"
              >
                <div class="task-upload-box task-upload-box--compact">
                  <img v-if="previewUrl(alt.image) || alt.existingImagePath"
                    :src="previewUrl(alt.image) ?? alt.existingImagePath!"
                    class="task-existing-preview" alt="Forhåndsvisning" />
                  <strong>{{ alt.image ? alt.image.name : (alt.existingImagePath ? 'Klikk for å erstatte bilde' : 'Klikk for å laste opp') }}</strong>
                  <p>{{ alt.image ? 'Ny fil valgt' : (alt.existingImagePath ? 'Eksisterende bilde' : 'Ingen fil valgt ennå') }}</p>
                  <input type="file" accept="image/*" @change="handleImageChange(alt, $event)" />
                </div>

                <label class="task-field">
                  <span>Bildetypen er</span>
                  <select v-model="alt.correctType">
                    <option value="ekte">Ekte</option>
                    <option value="ai-generert">KI-generert</option>
                    <option value="manipulert">Manipulert</option>
                  </select>
                </label>

                <button type="button" class="task-upload-remove" :disabled="draft.alternatives.length <= 2" @click="removeAlternative(index)">
                  Fjern
                </button>
              </article>

              <button v-if="draft.alternatives.length < 4" type="button" class="task-create-add task-create-add--column" @click="addAlternative">
                +
              </button>
            </div>
          </div>
        </template>

        <div class="task-template__actions task-template__actions--bottom">
          <button type="submit" class="task-template__save" :disabled="saveMutation.isPending.value">
            {{ saveMutation.isPending.value ? 'Lagrer...' : 'Lagre endringer' }}
          </button>
        </div>
      </form>
    </div>
  </SceneComponent>
</template>

<style scoped>
.task-create-page {
  padding: 1.5rem 2.5rem;
  overflow-y: auto;
}

.task-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 180px;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.14);
  color: white;
  font-weight: 700;
}

.task-state--error {
  color: #fecaca;
}

.task-create-panel {
  max-width: 980px;
  margin: 0 auto;
  background: transparent;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
  max-height: none;
  overflow-y: visible;
}

.task-create-panel--photo {
  max-width: 1400px;
}

.task-create-form {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.task-template {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.task-template__hero {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.task-template__actions {
  display: flex;
  gap: 0.8rem;
  align-items: center;
}

.task-template__actions--bottom {
  justify-content: flex-end;
  padding-top: 0.5rem;
}

.task-template__back,
.task-template__save {
  background: rgba(255, 255, 255, 0.95);
  color: white;
  border-color: rgba(15, 23, 42, 0.35);
  font-weight: 900;
  -webkit-text-stroke: 1.5px #000;
  paint-order: stroke fill;
  text-shadow:
    2px 0 #000,
    -2px 0 #000,
    0 2px #000,
    0 -2px #000,
    1px 1px #000,
    -1px -1px #000,
    1px -1px #000,
    -1px 1px #000;
}

.task-template__back {
  width: 3rem;
  min-width: 3rem;
  padding-inline: 0;
}

.task-template__save {
  min-width: 11rem;
}

.task-template__headline {
  text-align: center;
  margin: 0;
}

.task-template__headline h3 {
  margin: 0;
  color: white;
  font-size: 1.4rem;
  font-weight: 800;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.85);
  -webkit-text-stroke: 0;
}

.task-template__headline p {
  margin: 0.2rem 0 0;
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.9rem;
  font-weight: 500;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
}

.task-create-error {
  background: #fee2e2;
  border: 1px solid #fca5a5;
  color: #991b1b;
  padding: 0.8rem 1rem;
  border-radius: 0;
  font-weight: 700;
}

.task-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem;
}

.task-field-label {
  color: white;
  font-weight: 800;
  font-size: 0.9rem;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
}

.diff-selector {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.diff-btn {
  display: flex;
  align-items: center;
  gap: 1px;
  padding: 0.4rem 0.6rem;
  border: 2px solid #111111;
  border-radius: 0;
  background: white;
  cursor: pointer;
  font-size: 0.8rem;
  font-weight: 700;
  color: #6b7280;
  transition: all 0.15s ease;
}

.diff-btn:hover {
  border-color: #92400e;
  color: #92400e;
}

.diff-btn--active {
  border-color: #92400e;
  background: #92400e;
  color: white;
}

.diff-btn--ds {
  font-size: 0.75rem;
}

.diff-btn--ds.diff-btn--active {
  border-color: #1e40af;
  background: #1e40af;
}

.diff-star {
  color: #f59e0b;
  font-size: 0.9rem;
  line-height: 1;
}

.diff-btn--active .diff-star {
  color: white;
}

.task-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.task-field span {
  color: white;
  font-weight: 800;
  font-size: 0.9rem;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
}

.task-field input,
.task-field textarea,
.task-field select {
  background: white;
  border: 2px solid #111111;
  border-radius: 0;
  padding: 0.65rem 0.85rem;
  font-size: 0.95rem;
  font-weight: 500;
  color: #111827;
}

.task-field textarea {
  min-height: 2.8rem;
  resize: vertical;
}

.task-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.6rem;
}

.task-card-grid--news {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.task-card-grid--photo {
  grid-template-columns: repeat(auto-fit, minmax(280px, 320px));
  align-items: start;
}

.task-upload-card {
  background: rgba(255, 255, 255, 0.92);
  border: 2px solid #111;
  border-radius: 0;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.task-upload-card--good {
  border: 2px solid #10b981;
}

.task-upload-card--fake {
  border: 2px solid #f87171;
}

.task-card-badge {
  align-self: start;
  border-radius: 999px;
  padding: 0.24rem 0.75rem;
  background: white;
  border: 1px solid currentColor;
  font-size: 0.72rem;
  font-weight: 900;
  text-transform: uppercase;
}

.task-upload-card--good .task-card-badge {
  color: #047857;
}

.task-upload-card--fake .task-card-badge {
  color: #b91c1c;
}

.task-upload-box {
  position: relative;
  min-height: 260px;
  border: 2px dashed #cbd5e1;
  border-radius: 0;
  background: linear-gradient(180deg, #fff, #f8fafc);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  text-align: center;
  gap: 0.35rem;
  color: #64748b;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.15s, background 0.15s;
}

.task-upload-box:hover {
  border-color: #92400e;
  background: linear-gradient(180deg, #fff, #fef3c7);
}

.task-upload-box strong {
  color: #0f172a;
  font-size: 0.85rem;
  position: relative;
  z-index: 1;
}

.task-upload-box p {
  margin: 0;
  font-size: 0.75rem;
  position: relative;
  z-index: 1;
}

.task-upload-box input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}

.task-upload-box--compact {
  min-height: 220px;
}

.task-existing-preview {
  width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 0;
  opacity: 0.95;
  position: relative;
  z-index: 1;
  background: #f1f5f9;
}

.task-check {
  display: flex;
  gap: 0.55rem;
  align-items: center;
  color: #374151;
  font-weight: 600;
}

.task-check input {
  width: 1rem;
  height: 1rem;
  padding: 0;
}

.task-card-toggle {
  display: flex;
  gap: 1rem;
  margin-top: auto;
}

.task-create-add {
  align-self: start;
  background: #eff6ff;
  border-color: #111111;
  color: #1d4ed8;
}

.task-create-add--column {
  min-height: 220px;
  height: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-style: dashed;
  border-radius: 0;
  font-size: 2rem;
  background: rgba(255, 255, 255, 0.72);
}

.task-upload-remove {
  font-size: 1rem;
}

@media (max-width: 720px) {
  .task-create-panel,
  .task-create-panel--photo {
    max-width: 100%;
  }

  .task-field-grid {
    grid-template-columns: 1fr;
  }

  .task-card-grid--news,
  .task-card-grid--photo {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .task-card-grid--news .task-upload-card,
  .task-card-grid--photo .task-upload-card {
    width: 100%;
    max-width: 360px;
  }

  .task-template__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .task-template__save {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .task-create-page {
    padding: 0.5rem;
  }

  .diff-btn {
    min-height: 44px;
    padding: 0.5rem 0.75rem;
  }
}

@media (max-width: 1100px) {
  .task-card-grid--photo {
    grid-template-columns: repeat(auto-fit, minmax(240px, 280px));
  }
}
</style>
