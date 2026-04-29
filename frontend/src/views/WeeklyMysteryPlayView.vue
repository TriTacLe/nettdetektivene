<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import MysteryService from '@/services/MysteryService'
import type { MysterySubmissionResponseDTO } from '@/types'

const route = useRoute()
const router = useRouter()

const classroomId = computed(() => String(route.params.classroomId))
const allFeatured = ref<MysterySubmissionResponseDTO[]>([])
const isLoading = ref(true)
const errorMessage = ref('')

onMounted(async () => {
  try {
    allFeatured.value = await MysteryService.getAllFeaturedSubmissions(classroomId.value)
    if (allFeatured.value.length === 0) {
      errorMessage.value = 'Det er ingen aktiv oppgave denne uken. Sjekk tilbake senere.'
    }
  } catch {
    errorMessage.value = 'Kunne ikke laste mysteriene. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
})

const current = computed(() => allFeatured.value[0] ?? null)
const previous = computed(() => allFeatured.value.slice(1))

function categoryFromDesc(desc: string) {
  return desc.match(/^\[(.*?)]/)?.[1] ?? null
}

function cleanDesc(desc: string) {
  return desc.replace(/^\[.*?]\s*/, '')
}
</script>

<template>
  <SceneComponent
    title="Ukens mysterium"
    background="/StartBackground.jpg"
    @previous="router.push({ name: 'student-weekly-mystery', params: { classroomId } })"
  >
    <div class="flex flex-1 justify-center p-6 overflow-y-auto">
      <div class="w-full max-w-3xl flex flex-col gap-8">

        <div v-if="isLoading" class="state-msg">Laster oppgave...</div>
        <div v-else-if="errorMessage" class="state-msg state-msg--error">{{ errorMessage }}</div>

        <template v-else-if="current">
          <div class="mystery-card mystery-card--current">
            <div class="mystery-card__header">
              <p class="mystery-card__week">Uke {{ current.weekNumber }}</p>
              <span class="mystery-card__badge">Denne uken</span>
              <span v-if="categoryFromDesc(current.mysDesc)" class="mystery-card__tag">
                {{ categoryFromDesc(current.mysDesc) }}
              </span>
            </div>
            <h1 class="mystery-card__title">Hva er mistenkelig her?</h1>
            <p class="mystery-card__desc">{{ cleanDesc(current.mysDesc) }}</p>
            <div v-if="current.attachmentUrl" class="mystery-card__link-wrap">
              <a :href="current.attachmentUrl" target="_blank" rel="noreferrer noopener" class="mystery-card__link">
                Åpne eksempelet
              </a>
              <p class="mystery-card__link-note">Vær kritisk — hva avslører at dette er falskt eller manipulert?</p>
            </div>
            <div v-else class="mystery-card__no-link">
              Ingen lenke vedlagt. Les beskrivelsen og diskuter med klassen.
            </div>
          </div>

          <div v-if="previous.length > 0">
            <h2 class="previous-title">Tidligere ukers mysterier</h2>
            <div class="previous-list">
              <div v-for="item in previous" :key="item.id" class="mystery-card mystery-card--previous">
                <div class="mystery-card__header">
                  <p class="mystery-card__week">Uke {{ item.weekNumber }}, {{ item.weekYear }}</p>
                  <span v-if="categoryFromDesc(item.mysDesc)" class="mystery-card__tag">
                    {{ categoryFromDesc(item.mysDesc) }}
                  </span>
                </div>
                <p class="mystery-card__desc">{{ cleanDesc(item.mysDesc) }}</p>
                <a v-if="item.attachmentUrl" :href="item.attachmentUrl" target="_blank" rel="noreferrer noopener" class="mystery-card__link mystery-card__link--small">
                  Åpne eksempelet
                </a>
              </div>
            </div>
          </div>
        </template>

      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.state-msg {
  text-align: center;
  color: white;
  font-size: 1.1rem;
  font-weight: 700;
  padding: 3rem;
}
.state-msg--error { color: #fca5a5; }

.mystery-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.25);
  padding: 2rem;
}
.mystery-card--previous {
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.15);
  opacity: 0.9;
}

.mystery-card__header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-bottom: 1rem;
}
.mystery-card__week {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 800;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.mystery-card__badge {
  background: #132a34;
  color: white;
  border-radius: 7px;
  padding: 0.2rem 0.6rem;
  font-size: 0.75rem;
  font-weight: 800;
}
.mystery-card__tag {
  background: #e7e6f5;
  color: #475569;
  border-radius: 7px;
  padding: 0.2rem 0.6rem;
  font-size: 0.75rem;
  font-weight: 800;
}

.mystery-card__title {
  margin: 0 0 0.75rem;
  font-size: 1.8rem;
  font-weight: 900;
  color: #10212a;
}
.mystery-card__desc {
  margin: 0 0 1.5rem;
  font-size: 1rem;
  font-weight: 600;
  color: #334155;
  line-height: 1.6;
}
.mystery-card__link-wrap { display: flex; flex-direction: column; gap: 0.5rem; }
.mystery-card__link {
  display: inline-block;
  background: #132a34;
  color: white;
  border-radius: 10px;
  padding: 0.9rem 1.5rem;
  font-weight: 900;
  text-decoration: none;
  width: fit-content;
}
.mystery-card__link:hover { background: #1e3f50; }
.mystery-card__link--small { padding: 0.5rem 1rem; font-size: 0.875rem; }
.mystery-card__link-note {
  margin: 0;
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 600;
}
.mystery-card__no-link {
  background: #f1f5f9;
  border-radius: 10px;
  padding: 1rem 1.25rem;
  color: #475569;
  font-weight: 700;
  font-size: 0.9rem;
}

.previous-title {
  color: white;
  font-size: 1.2rem;
  font-weight: 800;
  margin: 0 0 1rem;
}
.previous-list { display: flex; flex-direction: column; gap: 1rem; }
</style>
