<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useMutation, useQuery } from '@tanstack/vue-query'
import {
  createClassroomMutation,
  updateClassroomMutation,
  getAllInstitutionsOptions,
  getClassroomOptions,
} from '@/generated-client/@tanstack/vue-query.gen.ts'

const router = useRouter()
const route = useRoute()

const classroomId = route.params.id as string | undefined
const isEditMode = !!classroomId

const title = ref('')
const classDesc = ref('')
const selectedInstitutionId = ref('')
const isGameEnabled = ref(false)
const serverError = ref<string | null>(null)

const { data: institutionsData, isLoading: isLoadingInstitutions } = useQuery(getAllInstitutionsOptions())

const institutions = computed(() => {
  const seen = new Set<string>()
  return (institutionsData.value?.data ?? []).filter((inst): inst is typeof inst & { instName: string; id: string } => {
    if (!inst.instName || !inst.id) return false
    if (seen.has(inst.instName)) return false
    seen.add(inst.instName)
    return true
  })
})

watch(institutions, (list) => {
  if (!isEditMode && list.length === 1 && list[0] && !selectedInstitutionId.value) {
    selectedInstitutionId.value = list[0].id
  }
})

// @ts-expect-error - Generated client types have complex type incompatibility
const { data: classroomData } = useQuery({
  ...getClassroomOptions({ path: { id: classroomId ?? '' } }),
  enabled: isEditMode && !!classroomId,
})

watch(classroomData, (data) => {
  const c = data?.data
  if (!c) return
  title.value = c.title ?? ''
  classDesc.value = c.classDesc ?? ''
  selectedInstitutionId.value = c.institutionId ?? ''
  isGameEnabled.value = c.isGameEnabled ?? false
}, { immediate: true })

interface FieldErrors {
  title?: string
  institutionId?: string
}
const fieldErrors = ref<FieldErrors>({})

function validate(): boolean {
  fieldErrors.value = {}
  if (!title.value.trim()) fieldErrors.value.title = 'Navn på klasserom kreves'
  if (!selectedInstitutionId.value) fieldErrors.value.institutionId = 'Velg en institusjon'
  return Object.keys(fieldErrors.value).length === 0
}

const createMutation = useMutation({
  ...createClassroomMutation(),
  onSuccess: (res) => {
    router.push({ name: 'classroom-detail', params: { id: res.data?.id } })
  },
  onError: (e: Error) => {
    serverError.value = e.message ?? 'Kunne ikke opprette klasserom'
  },
})

const updateMutation = useMutation({
  ...updateClassroomMutation(),
  onSuccess: () => {
    router.push({ name: 'classroom-detail', params: { id: classroomId } })
  },
  onError: (e: Error) => {
    serverError.value = e.message ?? 'Kunne ikke oppdatere klasserom'
  },
})

function submit() {
  serverError.value = null
  if (!validate()) return

  const body = {
    title: title.value.trim(),
    classDesc: classDesc.value.trim() || undefined,
    institutionId: selectedInstitutionId.value,
    gameEnabled: isGameEnabled.value,
  }

  if (isEditMode) {
    updateMutation.mutate({ path: { id: classroomId! }, body })
  } else {
    createMutation.mutate({ body })
  }
}
</script>

<template>
  <SceneComponent
    :title="isEditMode ? 'Rediger klasserom' : 'Opprett klasserom'"
    background="/StartBackground.jpg"
    :previous="isEditMode ? `/classroom/${classroomId}` : '/teacher-home'"
    dimOverlay
  >
    <div class="flex flex-col items-center justify-center flex-1 w-full px-6 pb-10 pt-4">
      <h2 class="form-heading">{{ isEditMode ? 'Rediger Klasserom' : 'Lag Klasserom' }}</h2>
      <div class="form-card">
        <div
          v-if="serverError"
          class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded text-sm"
        >
          {{ serverError }}
        </div>

        <form @submit.prevent="submit" class="form-fields">
          <div>
            <label class="field-label">Navn på klasserom</label>
            <input
              v-model="title"
              type="text"
              placeholder="f.eks 5A"
              class="field-input"
            />
            <p v-if="fieldErrors.title" class="field-error">
              {{ fieldErrors.title }}
            </p>
          </div>

          <div>
            <label class="field-label">Beskrivelse (valgfri)</label>
            <textarea
              v-model="classDesc"
              rows="3"
              placeholder="..."
              class="field-input field-textarea"
            />
          </div>

          <div>
            <label class="field-label">Institusjon</label>
            <select
              v-model="selectedInstitutionId"
              :disabled="isLoadingInstitutions"
              class="field-input"
            >
              <option value="" disabled>Velg institusjon...</option>
              <option
                v-for="inst in institutions"
                :key="inst.id"
                :value="inst.id"
              >
                {{ inst.instName }}
              </option>
            </select>
            <p v-if="isLoadingInstitutions" class="text-amber-200 text-xs mt-1">
              Laster institusjoner...
            </p>
            <p v-else-if="institutions.length === 0" class="field-error">
              Ingen institusjoner funnet. Kontakt administrator.
            </p>
            <p v-if="fieldErrors.institutionId" class="field-error">
              {{ fieldErrors.institutionId }}
            </p>
          </div>

          <label class="flex items-center gap-3 cursor-pointer">
            <input v-model="isGameEnabled" type="checkbox" class="w-5 h-5 accent-amber-500" />
            <span class="checkbox-label">Aktiver spill for klassen</span>
          </label>

          <button
            type="submit"
            class="btn-vn"
            :disabled="createMutation.isPending.value || updateMutation.isPending.value"
          >
            {{
              isEditMode
                ? updateMutation.isPending.value ? 'Lagrer...' : 'Lagre endringer'
                : createMutation.isPending.value ? 'Oppretter...' : 'Opprett klasserom'
            }}
          </button>
        </form>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.form-heading {
  font-size: 1.1rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  text-align: center;
  margin-bottom: 0.5rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  -webkit-text-stroke: 0;
  text-shadow: none;
  paint-order: normal;
}

.form-card {
  width: 100%;
  max-width: 28rem;
  margin: 0 auto;
  padding: 2rem;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0.875rem;
  backdrop-filter: blur(4px);
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 0.35rem;
  letter-spacing: 0.02em;
}

.field-input {
  width: 100%;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  padding: 0.65rem 1rem;
  font-size: 1rem;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.95);
  color: #111827;
}

.field-input:focus {
  outline: none;
  border-color: var(--color-nd-glow);
  box-shadow: 0 0 0 2px rgba(240, 192, 64, 0.3);
}

.field-textarea {
  resize: none;
}

.field-error {
  color: #fca5a5;
  font-size: 0.75rem;
  margin-top: 0.25rem;
}

.checkbox-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}
</style>
