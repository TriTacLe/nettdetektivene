<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import MysteryService from '@/services/MysteryService'
import { useStudentStore } from '@/stores/StudentStore'
import { useToast } from '@/composables/UseToast'

const route = useRoute()
const router = useRouter()
const studentStore = useStudentStore()
const toast = useToast()

const routeClassroomId = typeof route.params.classroomId === 'string' ? route.params.classroomId : null
const classroomId = ref<string | null>(routeClassroomId ?? studentStore.classroomId)

const image = ref<File | null>(null)
const imagePreview = ref<string | null>(null)
const alternative1 = ref('')
const alternative2 = ref('')
const alternative3 = ref('')
const correctIndex = ref(0)

const isSubmitting = ref(false)
const errorMessage = ref('')

const canSubmit = computed(
  () =>
    Boolean(
      classroomId.value &&
        image.value &&
        alternative1.value.trim() &&
        alternative2.value.trim() &&
        alternative3.value.trim(),
    ),
)

const previousPath = '/student-home'

function goBack() {
  router.push(previousPath)
}

function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  image.value = file
  if (imagePreview.value) URL.revokeObjectURL(imagePreview.value)
  imagePreview.value = file ? URL.createObjectURL(file) : null
}

onUnmounted(() => {
  if (imagePreview.value) URL.revokeObjectURL(imagePreview.value)
})

async function submitMystery() {
  if (!classroomId.value || !canSubmit.value || !image.value) return
  isSubmitting.value = true
  errorMessage.value = ''
  try {
    await MysteryService.submitMystery(
      {
        classroomId: classroomId.value,
        alternative1: alternative1.value.trim(),
        alternative2: alternative2.value.trim(),
        alternative3: alternative3.value.trim(),
        correctIndex: correctIndex.value,
      },
      image.value,
    )
    toast.showSuccess('Mysteriet er sendt til læreren')
    router.push(previousPath)
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : 'Kunne ikke sende inn mysteriet.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <SceneComponent
    title="Send inn mysterium"
    :previous="previousPath"
    background="/CorridorBackground.jpeg"
    @back="goBack"
  >
    <main class="submit-stage">
      <form class="submit-card" @submit.prevent="submitMystery">
        <h2 class="card-title">Send inn ditt mysterium</h2>
        <p class="card-intro">
          Last opp et bilde og skriv tre alternativer. Marker det som er riktig.
        </p>

        <label class="field">
          <span>Bilde</span>
          <div class="image-upload">
            <img v-if="imagePreview" :src="imagePreview" alt="Forhåndsvisning" class="image-preview" />
            <strong>{{ image ? image.name : 'Klikk for å laste opp' }}</strong>
            <p>{{ image ? 'Bildefil valgt' : 'Last opp et bilde til mysteriet' }}</p>
            <input type="file" accept="image/*" @change="handleImageChange" />
          </div>
        </label>

        <div class="alternatives">
          <label
            v-for="(_, i) in 3"
            :key="i"
            class="alt-row"
            :class="{ 'alt-row--marked': correctIndex === i }"
          >
            <input
              v-model.number="correctIndex"
              type="radio"
              :value="i"
              class="alt-radio"
              :name="'mystery-correct'"
            />
            <span class="alt-tag">{{ ['A', 'B', 'C'][i] }}</span>
            <input
              v-if="i === 0"
              v-model="alternative1"
              type="text"
              maxlength="500"
              placeholder="Skriv alternativ A"
              class="alt-input"
              required
            />
            <input
              v-else-if="i === 1"
              v-model="alternative2"
              type="text"
              maxlength="500"
              placeholder="Skriv alternativ B"
              class="alt-input"
              required
            />
            <input
              v-else
              v-model="alternative3"
              type="text"
              maxlength="500"
              placeholder="Skriv alternativ C"
              class="alt-input"
              required
            />
          </label>
        </div>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

        <div class="form-actions">
          <button type="submit" :disabled="!canSubmit || isSubmitting">
            {{ isSubmitting ? 'Sender...' : 'Send inn mysterium' }}
          </button>
        </div>
      </form>
    </main>
  </SceneComponent>
</template>

<style scoped>
.submit-stage {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 1.5rem 1.25rem;
  overflow-y: auto;
}

.submit-card {
  width: 100%;
  max-width: 820px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card-title {
  margin: 0;
  font-size: 1.65rem;
  font-weight: 800;
  color: white;
  text-align: center;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.85);
  -webkit-text-stroke: 0;
}

.card-intro {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 1rem;
  font-weight: 500;
  line-height: 1.45;
  text-align: center;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.field > span {
  color: white;
  font-weight: 800;
  font-size: 1rem;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
}

.image-upload {
  position: relative;
  min-height: 180px;
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

.image-upload:hover {
  border-color: #92400e;
  background: linear-gradient(180deg, #fff, #fef3c7);
}

.image-upload strong {
  color: #0f172a;
  font-size: 0.95rem;
  position: relative;
  z-index: 1;
}

.image-upload p {
  margin: 0;
  font-size: 0.82rem;
  position: relative;
  z-index: 1;
}

.image-upload input[type="file"] {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}

.image-preview {
  width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 0;
  background: #f1f5f9;
  position: relative;
  z-index: 1;
}

.alternatives {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.alt-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.6rem 0.8rem;
  border: 2px solid #111;
  border-radius: 0;
  background: white;
  transition: border-color 0.15s, background 0.15s;
  cursor: pointer;
}

.alt-row:hover { background: #fafafa; }

.alt-row--marked {
  border-color: #16a34a;
  background: #f0fdf4;
}

.alt-radio {
  width: 1.1rem;
  height: 1.1rem;
  accent-color: #16a34a;
  cursor: pointer;
  flex-shrink: 0;
}

.alt-tag {
  width: 1.7rem;
  height: 1.7rem;
  border-radius: 0;
  background: #f1f5f9;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 800;
  flex-shrink: 0;
}

.alt-row--marked .alt-tag {
  background: #16a34a;
  color: white;
}

.alt-input {
  flex: 1;
  min-width: 0;
  background: transparent;
  color: #0f172a;
  border: 0;
  outline: 0;
  padding: 0.4rem 0.2rem;
  font: inherit;
  font-size: 0.95rem;
  font-weight: 500;
}

.alt-input::placeholder {
  color: #94a3b8;
}

.error {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 700;
  color: #991b1b;
  background: #fee2e2;
  border: 1px solid #fca5a5;
  padding: 0.55rem 0.8rem;
  border-radius: 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.25rem;
}
</style>
