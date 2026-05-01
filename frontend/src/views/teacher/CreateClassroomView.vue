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
      <div class="form-card">
        <h2 class="form-heading no-stroke">{{ isEditMode ? 'Rediger Klasserom' : 'Lag Klasserom' }}</h2>

        <div
          v-if="serverError"
          class="form-feedback form-feedback--error"
        >
          {{ serverError }}
        </div>

        <form @submit.prevent="submit" class="form-fields">
          <div>
            <label class="field-label" for="classroom-title">Navn på klasserom</label>
            <input
              id="classroom-title"
              v-model="title"
              type="text"
              placeholder="f.eks 5A"
              class="field-input"
            />
            <p v-if="fieldErrors.title" class="field-error" role="alert">
              {{ fieldErrors.title }}
            </p>
          </div>

          <div>
            <label class="field-label" for="classroom-desc">Beskrivelse (valgfri)</label>
            <textarea
              id="classroom-desc"
              v-model="classDesc"
              rows="3"
              placeholder="..."
              class="field-input field-textarea"
            />
          </div>

          <div>
            <label class="field-label" for="classroom-institution">Institusjon</label>
            <select
              id="classroom-institution"
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
            <p v-if="isLoadingInstitutions" class="field-hint">
              Laster institusjoner...
            </p>
            <p v-else-if="institutions.length === 0" class="field-error" role="alert">
              Ingen institusjoner funnet. Kontakt administrator.
            </p>
            <p v-if="fieldErrors.institutionId" class="field-error" role="alert">
              {{ fieldErrors.institutionId }}
            </p>
          </div>

          <label class="checkbox-row">
            <input
              v-model="isGameEnabled"
              type="checkbox"
              class="checkbox-input"
              tabindex="0"
              @keydown.enter.prevent="isGameEnabled = !isGameEnabled"
            />
            <span class="checkbox-label">Aktiver spill for klassen</span>
          </label>

          <button
            type="submit"
            tabindex="0"
            class="btn-bare form-submit"
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
.form-card {
  width: 100%;
  max-width: 28rem;
  margin: 0 auto;
  padding: 1.5rem;
  background: #faf7f2;
  border: 1px solid #d6d3d1;
  border-radius: 1rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-heading {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  color: #44403c;
  text-align: center;
  margin: 0;
  letter-spacing: 0.03em;
}

.form-feedback--error {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 0.875rem;
  font-weight: 600;
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field-label {
  display: block;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 0.95rem;
  font-weight: 700;
  color: #1c1917;
  margin-bottom: 0.35rem;
}

.field-input {
  width: 100%;
  border: 1.5px solid #a8a29e;
  border-radius: 0.5rem;
  padding: 0.625rem 0.875rem;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 0.95rem;
  font-weight: 500;
  background: white;
  color: #1c1917;
  box-sizing: border-box;
}

.field-input:focus {
  outline: 3px solid #92400e;
  outline-offset: 2px;
  border-color: #92400e;
}

.field-textarea {
  resize: none;
}

.field-error {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  color: #b91c1c;
  font-size: 0.8rem;
  font-weight: 600;
  margin-top: 0.25rem;
}

.field-hint {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  color: #78716c;
  font-size: 0.8rem;
  margin-top: 0.25rem;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
}

.checkbox-input {
  width: 1.25rem;
  height: 1.25rem;
  accent-color: #92400e;
  flex-shrink: 0;
  cursor: pointer;
}

.checkbox-input:focus {
  outline: 3px solid #92400e;
  outline-offset: 2px;
}

.checkbox-label {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 0.95rem;
  font-weight: 700;
  color: #44403c;
}

.form-submit {
  width: 100%;
  padding: 0.75rem;
  background: #92400e;
  color: white;
  border: 2px solid #78350f;
  border-radius: 0.75rem;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
}

.form-submit:hover {
  background: #78350f;
}

.form-submit:focus {
  outline: 3px solid #92400e;
  outline-offset: 2px;
}

.form-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
