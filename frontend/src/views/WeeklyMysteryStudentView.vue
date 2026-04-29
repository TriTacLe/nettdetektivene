<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import MysteryService from '@/services/MysteryService'
import type { MysteryStatus, MysterySubmissionResponseDTO } from '@/types'

const router = useRouter()
const route = useRoute()
const categories = ['Falsk nyhet', 'Manipulert bilde', 'Mistenkelig nettside']
const classroomId = computed(() => String(route.params.classroomId))
const selectedCategory = ref(categories[0])
const exampleUrl = ref('')
const description = ref('')
const submissions = ref<MysterySubmissionResponseDTO[]>([])
const featuredSubmission = ref<MysterySubmissionResponseDTO | null>(null)
const isLoading = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const currentWeek = computed(() => submissions.value[0]?.weekNumber ?? featuredSubmission.value?.weekNumber ?? getIsoWeek(new Date()))
const weeklyPrompt = computed(() => featuredSubmission.value?.mysDesc || 'Har du sett en falsk nyhet på nettet denne uken?')
const visibleSubmissions = computed(() => [...submissions.value].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt)).slice(0, 4))
const canSubmit = computed(() => Boolean(selectedCategory.value && description.value.trim()))

onMounted(loadMysteryData)

async function loadMysteryData() {
  isLoading.value = true
  errorMessage.value = ''
  try {
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
  if (!canSubmit.value) return
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
  return Math.ceil((((target.getTime() - yearStart.getTime()) / 86400000) + 1) / 7)
}
</script>

<template>
  <SceneComponent title="Ukens Mysterium" background="/NyhetskvartaletBackground.png" @previous="router.back()">
    <main class="mystery-page">
      <section class="mystery-layout">
        <div class="left-column">
          <article class="card intro-card">
            <div class="card-header">🔍 Ukens Mysterium - Uke {{ currentWeek }}</div>
            <div class="card-body">
              <h2>{{ weeklyPrompt }}</h2>
              <p>Send inn et eksempel du har funnet på nettet – en falsk nyhet, et manipulert bilde eller en mistenkelig nettside. Læreren din godkjenner de beste bidragene.</p>
              <div class="reward-row"><span class="pill pill--deadline">Uke {{ currentWeek }}</span><span class="pill pill--reward">🏆 Premie: Kildekritiker-medalje</span></div>
            </div>
          </article>
          <form class="card form-card" @submit.prevent="submitMystery">
            <h2>Send inn ditt eksempel</h2>
            <fieldset class="category-group">
              <legend>Hva slags eksempel er dette?</legend>
              <label v-for="category in categories" :key="category" class="category-option" :class="{ active: selectedCategory === category }"><input v-model="selectedCategory" type="radio" :value="category" /><span>{{ category }}</span></label>
            </fieldset>
            <label class="field-label" for="example-url">Lenke til eksempelet (URL):</label>
            <input id="example-url" v-model="exampleUrl" class="text-input" type="url" placeholder="https://..." />
            <label class="field-label" for="description">Beskriv kort hva som er mistenkelig:</label>
            <textarea id="description" v-model="description" class="text-area" rows="4" maxlength="500" placeholder="F.eks. «Overskriften er veldig dramatisk og kilden er ukjent...»" required />
            <p v-if="errorMessage" class="message error">{{ errorMessage }}</p><p v-if="successMessage" class="message success">{{ successMessage }}</p>
            <button class="submit-button" type="submit" :disabled="!canSubmit || isSubmitting">{{ isSubmitting ? 'Sender...' : 'Send inn eksempel →' }}</button>
          </form>
        </div>
        <aside class="card submissions-card">
          <div class="submissions-header">Mine innsendte eksempler</div>
          <div v-if="isLoading" class="empty-state">Laster...</div><div v-else-if="visibleSubmissions.length === 0" class="empty-state">Du har ikke sendt inn noen eksempler ennå.</div>
          <div v-else class="submission-list"><article v-for="submission in visibleSubmissions" :key="submission.id" class="submission-item"><p>Uke {{ submission.weekNumber }}</p><span class="type-chip">{{ categoryFromSubmission(submission) }}</span><h3>{{ cleanDescription(submission) }}</h3><div class="submission-footer"><a v-if="submission.attachmentUrl" :href="submission.attachmentUrl" target="_blank" rel="noreferrer">Åpne lenke</a><span v-else /><span class="status-chip" :class="submission.mysStatus.toLowerCase()">{{ statusText(submission.mysStatus) }}</span></div></article></div>
        </aside>
      </section>
    </main>
  </SceneComponent>
</template>

<style scoped>
.mystery-page{flex:1;display:flex;justify-content:center;padding:1.75rem 2rem 2.5rem}.mystery-layout{width:min(100%,980px);display:grid;grid-template-columns:minmax(0,1.25fr) minmax(280px,.8fr);gap:2.5rem;align-items:start}.left-column{display:flex;flex-direction:column;gap:1rem}.card{background:#fff;border-radius:14px;box-shadow:0 12px 24px rgba(0,0,0,.28);overflow:hidden}.card-header,.submissions-header{background:#132a34;color:#fff;padding:.8rem 1.4rem;text-align:center;font-weight:800}.card-body,.form-card{padding:1.35rem 1.55rem}.intro-card h2,.form-card h2{margin:0 0 1rem;color:#10212a;font-size:1.3rem;font-weight:900}.intro-card p{color:#64748b;line-height:1.45;margin:0 0 1.5rem}.reward-row,.category-group{display:flex;flex-wrap:wrap;gap:.6rem}.pill{border-radius:8px;padding:.45rem .75rem;font-size:.78rem;font-weight:800}.pill--deadline{background:#fff4df;color:#b45309}.pill--reward{background:#e9f9ef;color:#15803d}.form-card{display:flex;flex-direction:column;gap:.75rem}.category-group{border:0;padding:0;margin:0}.category-group legend,.field-label{flex-basis:100%;color:#1f2937;font-size:.85rem;font-weight:800}.category-option{display:inline-flex;align-items:center;gap:.35rem;border:1px solid #d5d9e5;border-radius:8px;padding:.55rem .85rem;background:#f7f7fb;color:#334155;font-size:.8rem;font-weight:800;cursor:pointer}.category-option.active{background:#132a34;border-color:#132a34;color:white}.category-option input{accent-color:#132a34}.text-input,.text-area{width:100%;border:1px solid #ccd3df;border-radius:9px;background:#f7f8fc;color:#10212a;font:inherit;padding:.8rem .9rem}.text-area{resize:vertical;min-height:6rem}.submit-button{border:0;border-radius:10px;background:#132a34;color:white;font-weight:900;padding:1rem;cursor:pointer}.submit-button:disabled{cursor:not-allowed;opacity:.55}.submission-list{display:flex;flex-direction:column;gap:.85rem;padding:1rem}.submission-item{border:1px solid #d7dce7;border-radius:12px;background:#f8f8fc;padding:.9rem 1rem}.submission-item p{margin:0 0 .35rem;color:#94a3b8;font-size:.78rem;font-weight:800}.type-chip{display:inline-block;min-width:8rem;border-radius:7px;background:#e7e6f5;color:#475569;padding:.25rem .6rem;font-size:.72rem;font-weight:800;text-align:center}.submission-item h3{margin:.55rem 0 1rem;color:#10212a;font-size:.9rem;font-weight:900}.submission-footer{display:flex;align-items:center;justify-content:space-between;gap:.75rem}.status-chip{border:2px solid currentColor;border-radius:8px;padding:.35rem .7rem;font-size:.78rem;font-weight:900}.approved{color:#16a34a;background:#ecfdf3}.rejected{color:#dc2626;background:#fef2f2}.pending{color:#d97706;background:#fff7ed}.empty-state{color:#64748b;padding:2rem 1.25rem;text-align:center;font-weight:700}.message{margin:0;font-size:.85rem;font-weight:800}.error{color:#dc2626}.success{color:#15803d}@media(max-width:850px){.mystery-page{padding-inline:1rem}.mystery-layout{grid-template-columns:1fr;gap:1rem}}
</style>
