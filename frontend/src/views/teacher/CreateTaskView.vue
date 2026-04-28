<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { getClassroomOptions } from '@/generated-client/@tanstack/vue-query.gen'
import { createTeacherTask } from '@/services/TaskService'
import { TaskType, type TeacherNewsTaskDraft, type TeacherPhotoTaskDraft, type TeacherPhotoAlternativeDraft } from '@/types'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const classroomId = route.params.id as string

const { data: classroomData } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroom = computed(() => classroomData.value?.data ?? null)

const selectedType = computed<TaskType.NEWS | TaskType.PHOTO | null>(() => {
  const queryType = route.query.type
  if (queryType === TaskType.NEWS || queryType === TaskType.PHOTO) {
    return queryType
  }
  return null
})

const serverError = ref<string | null>(null)

const newsDraft = ref<TeacherNewsTaskDraft>({
  type: TaskType.NEWS,
  title: '',
  difficulty: 1,
  stopLocation: 'nyhetskvartalet',
  question: '',
  hint: '',
  articles: [
    { image: null, isReal: true },
    { image: null, isReal: false },
  ],
})

const photoDraft = ref<TeacherPhotoTaskDraft>({
  type: TaskType.PHOTO,
  title: '',
  difficulty: 1,
  stopLocation: 'fotografen',
  description: '',
  hint: '',
  alternatives: [
    makePhotoAlternative(),
    makePhotoAlternative(),
    makePhotoAlternative(),
  ],
})

const saveMutation = useMutation({
  mutationFn: () => {
    if (selectedType.value === TaskType.NEWS) {
      validateNewsDraft()
      return createTeacherTask(classroomId, newsDraft.value)
    }
    if (selectedType.value === TaskType.PHOTO) {
      validatePhotoDraft()
      return createTeacherTask(classroomId, photoDraft.value)
    }
    throw new Error('Velg oppgavetype først')
  },
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['teacher-classroom-tasks', classroomId] })
    router.push({ name: 'classroom-tasks', params: { id: classroomId } })
  },
  onError: (error: Error) => {
    serverError.value = error.message || 'Kunne ikke opprette oppgaven'
  },
})

function setType(type: TaskType.NEWS | TaskType.PHOTO) {
  serverError.value = null
  router.replace({ name: 'classroom-task-create', params: { id: classroomId }, query: { type } })
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

function addPhotoAlternative() {
  if (photoDraft.value.alternatives.length >= 4) {
    serverError.value = 'Maksimalt 4 bildealternativer tillatt'
    return
  }
  serverError.value = null
  photoDraft.value.alternatives.push(makePhotoAlternative())
}

function removePhotoAlternative(index: number) {
  if (photoDraft.value.alternatives.length <= 2) return
  photoDraft.value.alternatives.splice(index, 1)
}

function submit() {
  serverError.value = null
  saveMutation.mutate()
}

function validateNewsDraft() {
  if (!newsDraft.value.title.trim()) throw new Error('Oppgaven må ha en tittel')
  if (!newsDraft.value.question.trim()) throw new Error('Oppgaven må ha et spørsmål')
  if (newsDraft.value.articles.some(article => !article.image)) throw new Error('Last opp begge artikkelbildene')
  if (!newsDraft.value.articles.some(article => article.isReal) || !newsDraft.value.articles.some(article => !article.isReal)) {
    throw new Error('Du må ha én ekte og én falsk artikkel')
  }
}

function validatePhotoDraft() {
  if (!photoDraft.value.title.trim()) throw new Error('Oppgaven må ha en tittel')
  if (!photoDraft.value.description.trim()) throw new Error('Oppgaven må ha en beskrivelse')
  if (photoDraft.value.alternatives.length < 2) throw new Error('Legg til minst to bildealternativer')
  for (const alternative of photoDraft.value.alternatives) {
    if (!alternative.image) throw new Error('Alle alternativer må ha et bilde')
    if (!alternative.description.trim()) throw new Error('Alle alternativer må ha en beskrivelse')
    if (!alternative.begrunnelserText.trim()) throw new Error('Alle alternativer må ha minst én begrunnelse')
  }
}

function activeDraft() {
  return selectedType.value === TaskType.NEWS ? newsDraft.value : photoDraft.value
}

function defaultStop() {
  return selectedType.value === TaskType.NEWS ? 'nyhetskvartalet' : 'fotografen'
}

function selectDifficulty(n: number) {
  const d = activeDraft()
  d.difficulty = n
  d.stopLocation = defaultStop()
}

function selectDatasenteret() {
  const d = activeDraft()
  d.difficulty = 4
  d.stopLocation = 'datasenteret'
}

function makePhotoAlternative(): TeacherPhotoAlternativeDraft {
  return {
    image: null,
    description: '',
    correctType: 'ekte',
    begrunnelserText: '',
  }
}
</script>

<template>
  <SceneComponent
    :title="classroom?.title ? `Ny oppgave: ${classroom.title}` : 'Ny oppgave'"
    :previous="selectedType ? `/classroom/${classroomId}/tasks` : `/classroom/${classroomId}`"
    background="/corkboard.jpg"
  >
    <div class="task-create-page">
      <section v-if="!selectedType" class="task-create-panel task-create-panel--picker">
        <div>
          <p class="task-create-eyebrow">Velg oppgavetype</p>
          <h2 class="task-create-heading">Hvilken stopp skal denne oppgaven høre til?</h2>
        </div>

        <div class="task-type-grid">
          <button class="task-type-card" @click="setType(TaskType.NEWS)">
            <span class="task-type-tag">Nyhetskvartalet</span>
            <strong>Bildesammenligning av nyhetsartikler</strong>
            <p>Last opp to artikkelbilder og marker hvilken som er ekte.</p>
          </button>

          <button class="task-type-card task-type-card--photo" @click="setType(TaskType.PHOTO)">
            <span class="task-type-tag">Fotografen</span>
            <strong>Klassifisering av bilder</strong>
            <p>Last opp flere bilder, skriv beskrivelser og legg inn riktige kategorier.</p>
          </button>
        </div>
      </section>

      <form v-else class="task-create-panel task-create-form" :class="{ 'task-create-panel--photo': selectedType === TaskType.PHOTO }" @submit.prevent="submit">
        <div class="task-create-topbar">
          <div>
            <p class="task-create-eyebrow">Oppgavemal</p>
            <h2 class="task-create-heading">
              {{ selectedType === TaskType.NEWS ? 'Nyhetskvartalet-mal' : 'Fotografen-mal' }}
            </h2>
          </div>
          <button type="button" class="task-create-switch" @click="router.replace({ name: 'classroom-task-create', params: { id: classroomId } })">
            Bytt type
          </button>
        </div>

        <div v-if="serverError" class="task-create-error">{{ serverError }}</div>

        <template v-if="selectedType === TaskType.NEWS">
          <div class="task-template task-template--news">
            <div class="task-template__hero">
              <div class="task-template__actions">
                <button type="button" class="task-template__back" @click="router.push({ name: 'classroom-tasks', params: { id: classroomId } })">
                  ←
                </button>
                <button type="submit" class="task-template__save" :disabled="saveMutation.isPending.value">
                  {{ saveMutation.isPending.value ? 'Lagrer...' : 'Lagre Oppgave' }}
                </button>
              </div>
              <div class="task-template__headline">
                <h3>Nyhetskvartal template</h3>
                <p>Last opp én god og én falsk nyhetsartikkel. Elevene velger hvilken de tror er ekte.</p>
              </div>
              <div class="task-field-grid">
                <label class="task-field">
                  <span>Tittel</span>
                  <input v-model="newsDraft.title" type="text" />
                </label>
                <div class="task-field">
                  <span class="task-field-label">Vanskelighetsgrad</span>
                  <div class="diff-selector">
                    <button v-for="n in 3" :key="n" type="button"
                      :class="['diff-btn', { 'diff-btn--active': newsDraft.difficulty === n && newsDraft.stopLocation !== 'datasenteret' }]"
                      @click="selectDifficulty(n)">
                      <span v-for="i in 3" :key="i" :class="['diff-star', { 'diff-star--on': i <= n }]">★</span>
                    </button>
                    <button type="button"
                      :class="['diff-btn diff-btn--ds', { 'diff-btn--active': newsDraft.stopLocation === 'datasenteret' }]"
                      @click="selectDatasenteret()">
                      Datasenteret
                    </button>
                  </div>
                </div>
              </div>
              <label class="task-field">
                <span>Spørsmål</span>
                <textarea v-model="newsDraft.question" rows="2" />
              </label>
              <label class="task-field">
                <span>Hint</span>
                <textarea v-model="newsDraft.hint" rows="2" />
              </label>
            </div>

            <div class="task-card-grid task-card-grid--news">
              <article
                v-for="(article, index) in newsDraft.articles"
                :key="index"
                class="task-upload-card task-upload-card--news"
                :class="article.isReal ? 'task-upload-card--good' : 'task-upload-card--fake'"
              >
                <span class="task-card-badge">
                  {{ article.isReal ? '✓ God artikkel' : '✕ Falsk artikkel' }}
                </span>
                <div class="task-upload-box">
                  <img v-if="previewUrl(article.image)" :src="previewUrl(article.image)!" class="task-existing-preview" alt="Forhåndsvisning" />
                  <strong>{{ article.image ? article.image.name : 'Klikk for å laste opp' }}</strong>
                  <p>{{ article.image ? 'Bildefil valgt' : 'Last opp bilde av artikkel' }}</p>
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

        <template v-else>
          <div class="task-template task-template--photo">
            <div class="task-template__hero">
              <div class="task-template__actions">
                <button type="button" class="task-template__back" @click="router.push({ name: 'classroom-tasks', params: { id: classroomId } })">
                  ←
                </button>
                <button type="submit" class="task-template__save" :disabled="saveMutation.isPending.value">
                  {{ saveMutation.isPending.value ? 'Lagrer...' : 'Lagre Oppgave' }}
                </button>
              </div>
              <div class="task-template__headline">
                <h3>Fotoboksen oppgave template</h3>
                <p>Last opp bilder. Elevene velger for hvert bilde: Ekte / KI-generert / Manipulert.</p>
              </div>
              <div class="task-field-grid">
                <label class="task-field">
                  <span>Tittel</span>
                  <input v-model="photoDraft.title" type="text" />
                </label>
                <div class="task-field">
                  <span class="task-field-label">Vanskelighetsgrad</span>
                  <div class="diff-selector">
                    <button v-for="n in 3" :key="n" type="button"
                      :class="['diff-btn', { 'diff-btn--active': photoDraft.difficulty === n && photoDraft.stopLocation !== 'datasenteret' }]"
                      @click="selectDifficulty(n)">
                      <span v-for="i in 3" :key="i" :class="['diff-star', { 'diff-star--on': i <= n }]">★</span>
                    </button>
                    <button type="button"
                      :class="['diff-btn diff-btn--ds', { 'diff-btn--active': photoDraft.stopLocation === 'datasenteret' }]"
                      @click="selectDatasenteret()">
                      Datasenteret
                    </button>
                  </div>
                </div>
              </div>
              <label class="task-field">
                <span>Oppgavetekst</span>
                <textarea v-model="photoDraft.description" rows="2" />
              </label>
              <label class="task-field">
                <span>Hint</span>
                <textarea v-model="photoDraft.hint" rows="2" />
              </label>
            </div>

            <div class="task-card-grid task-card-grid--photo">
              <article
                v-for="(alternative, index) in photoDraft.alternatives"
                :key="index"
                class="task-upload-card task-upload-card--photo"
              >
                <div class="task-upload-box task-upload-box--compact">
                  <img v-if="previewUrl(alternative.image)" :src="previewUrl(alternative.image)!" class="task-existing-preview" alt="Forhåndsvisning" />
                  <strong>{{ alternative.image ? alternative.image.name : 'Klikk for å laste opp' }}</strong>
                  <p>{{ alternative.image ? 'Bildefil valgt' : 'Ingen fil valgt ennå' }}</p>
                  <input type="file" accept="image/*" @change="handleImageChange(alternative, $event)" />
                </div>

                <label class="task-field">
                  <span>Beskrivelse</span>
                  <input v-model="alternative.description" type="text" placeholder="Legg til kontekst for elevene..." />
                </label>

                <label class="task-field">
                  <span>Bildetypen er</span>
                  <select v-model="alternative.correctType">
                    <option value="ekte">Ekte</option>
                    <option value="ai-generert">KI-generert</option>
                    <option value="manipulert">Manipulert</option>
                  </select>
                </label>

                <label class="task-field">
                  <span>Begrunnelser, én per linje</span>
                  <textarea v-model="alternative.begrunnelserText" rows="4" />
                </label>

                <button type="button" class="task-upload-remove" :disabled="photoDraft.alternatives.length <= 2" @click="removePhotoAlternative(index)">
                  Fjern
                </button>
              </article>

              <button v-if="photoDraft.alternatives.length < 4" type="button" class="task-create-add task-create-add--column" @click="addPhotoAlternative">
                +
              </button>
            </div>
          </div>
        </template>
      </form>
    </div>
  </SceneComponent>
</template>

<style scoped>
.task-create-page {
  padding: 1.5rem;
  overflow-y: auto;
}

.task-create-panel {
  max-width: 1080px;
  margin: 0 auto;
  background: linear-gradient(180deg, rgba(255, 250, 240, 0.96), rgba(255, 255, 255, 0.94));
  border: 2px solid rgba(154, 52, 18, 0.24);
  border-radius: 1.5rem;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.18);
  padding: 1.5rem;
  max-height: calc(100vh - 8rem);
  overflow-y: auto;
}

.task-create-panel--photo {
  max-width: 1400px;
}

.task-create-panel--picker {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.task-create-eyebrow {
  margin: 0 0 0.35rem;
  color: #9a3412;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.82rem;
  font-weight: 800;
}

.task-create-heading {
  margin: 0;
  color: #ffffff;
  font-size: clamp(1.4rem, 2vw, 2rem);
  font-weight: 900;
  -webkit-text-stroke: 5px #000000;
  paint-order: stroke fill;
}

.task-type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1rem;
}

.task-type-card {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  align-items: start;
  background: radial-gradient(circle at top left, #fef3c7, #fdba74);
  border-color: #b45309;
  color: #431407;
  text-align: left;
}

.task-type-card--photo {
  background: radial-gradient(circle at top left, #dcfce7, #86efac);
  border-color: #15803d;
  color: #052e16;
}

.task-type-tag {
  display: inline-flex;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  font-size: 0.8rem;
  font-weight: 800;
}

.task-create-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.task-template {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.task-template__hero {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.task-template__actions {
  display: flex;
  gap: 0.8rem;
  align-items: center;
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
  border-radius: 999px;
}

.task-template__save {
  min-width: 11rem;
}

.task-template__headline {
  text-align: center;
  margin: 0.2rem 0 0.5rem;
}

.task-template__headline h3 {
  margin: 0;
  color: white;
  font-size: clamp(1.6rem, 2.2vw, 2.35rem);
  font-weight: 900;
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
  -webkit-text-stroke: 0;
}

.task-template__headline p {
  margin: 0.6rem 0 0;
  color: rgba(49, 46, 129, 0.85);
  font-size: 0.95rem;
  font-weight: 700;
}

.task-create-topbar {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.task-create-switch,
.task-create-add,
.task-create-submit,
.task-upload-remove {
  font-size: 1rem;
}

.task-create-switch {
  background: white;
  color: white;
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

.task-create-error {
  background: #fee2e2;
  border: 1px solid #fca5a5;
  color: #991b1b;
  padding: 0.8rem 1rem;
  border-radius: 0.9rem;
  font-weight: 700;
}

.task-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.task-field-label {
  color: #7c2d12;
  font-weight: 800;
  font-size: 0.95rem;
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
  border: 2px solid #d1d5db;
  border-radius: 0.6rem;
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
  color: #d1d5db;
  font-size: 0.9rem;
  line-height: 1;
}

.diff-star--on {
  color: #f59e0b;
}

.diff-btn--active .diff-star--on {
  color: white;
}

.task-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.task-field span {
  color: #7c2d12;
  font-weight: 800;
  font-size: 0.95rem;
}

.task-field input,
.task-field textarea,
.task-field select {
  background: white;
  border: 2px solid #d1d5db;
  border-radius: 0.8rem;
  padding: 0.85rem 1rem;
  font-size: 1rem;
  font-weight: 500;
  color: #111827;
}

.task-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
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
  border: 1px solid #fed7aa;
  border-radius: 1rem;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.task-upload-card--news {
  min-height: 0;
}

.task-upload-card--good {
  border: 2px solid #10b981;
}

.task-upload-card--fake {
  border: 2px solid #f87171;
}

.task-upload-card h3 {
  margin: 0;
  color: #111827;
  -webkit-text-stroke: 0;
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

.task-upload-card__header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
}

.task-upload-box {
  position: relative;
  min-height: 110px;
  border: 2px dashed #cbd5e1;
  border-radius: 0.85rem;
  background: linear-gradient(180deg, #fff, #f8fafc);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  text-align: center;
  gap: 0.35rem;
  color: #64748b;
}

.task-upload-box strong {
  color: #0f172a;
  font-size: 0.95rem;
  position: relative;
  z-index: 1;
}

.task-upload-box p {
  margin: 0;
  font-size: 0.82rem;
  position: relative;
  z-index: 1;
}

.task-upload-box input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.task-upload-box--compact {
  min-height: 96px;
}

.task-existing-preview {
  width: 100%;
  max-height: 80px;
  object-fit: cover;
  border-radius: 0.5rem;
  opacity: 0.85;
  position: relative;
  z-index: 1;
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
  border-color: #3b82f6;
  color: #1d4ed8;
}

.task-create-add--column {
  min-height: 220px;
  height: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-style: dashed;
  font-size: 2rem;
  background: rgba(255, 255, 255, 0.72);
}

.task-create-submit {
  align-self: end;
  background: #9a3412;
  border-color: #7c2d12;
  color: white;
}

.task-create-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .task-create-panel,
  .task-create-panel--photo {
    max-width: 100%;
  }

  .task-field-grid,
  .task-create-topbar {
    grid-template-columns: 1fr;
    display: grid;
  }

  .task-card-grid--news,
  .task-card-grid--photo {
    grid-template-columns: 1fr;
  }

  .task-template__actions,
  .task-create-switch,
  .task-create-add {
    width: 100%;
  }

  .task-template__actions {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 1100px) {
  .task-card-grid--photo {
    grid-template-columns: repeat(auto-fit, minmax(240px, 280px));
  }
}

.difficulty-picker {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.5rem;
}

.difficulty-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  padding: 0.6rem 0.4rem;
  border-radius: 0.75rem;
  border: 2px solid #d1d5db;
  background: white;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  font-size: 0.8rem;
  color: #374151;
}

.difficulty-stars {
  font-size: 1rem;
  color: #f59e0b;
  letter-spacing: -0.05em;
}

.difficulty-option--4 .difficulty-stars {
  color: #6366f1;
  font-size: 1.1rem;
}

.difficulty-label {
  font-size: 0.7rem;
  font-weight: 700;
  text-align: center;
  line-height: 1.2;
}

.difficulty-option--active {
  border-color: #9a3412;
  background: #fff7ed;
}

.difficulty-option--4.difficulty-option--active {
  border-color: #6366f1;
  background: #eef2ff;
}

@media (max-width: 720px) {
  .difficulty-picker {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
