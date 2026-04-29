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
  onError: (e: any) => {
    serverError.value = e.message ?? 'Kunne ikke opprette klasserom'
  },
})

const updateMutation = useMutation({
  ...updateClassroomMutation(),
  onSuccess: () => {
    router.push({ name: 'classroom-detail', params: { id: classroomId } })
  },
  onError: (e: any) => {
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
    :previous="isEditMode ? `/classroom/${classroomId}` : '/classroom-overview'"
  >
    <div class="min-h-screen flex flex-col items-center justify-center p-8">
      <div class="bg-amber-50 rounded-2x1 shadow-xl p-8 w-full max-w-md">
        <h1 class="text-2xl font-bold text-amber-900 mb-6">
          {{ isEditMode ? 'Rediger klasserom' : 'Opprett klasserom' }}
        </h1>

        <div
          v-if="serverError"
          class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded mb-4 text-sm"
        >
          {{ serverError }}
        </div>

        <form @submit.prevent="submit" class="flex flex-col gap-5">
          <div>
            <label class="block text-sm font-semibold text-amber-900 mb-1">
              Navn på klasserom
            </label>
            <input
              v-model="title"
              type="text"
              placeholder="f.eks 5A"
              class="w-full border border-amber-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
            <p v-if="fieldErrors.title" class="text-red-600 text-xs mt-1">
              {{ fieldErrors.title }}
            </p>
          </div>

          <div>
            <label class="block text-sm font-semibold text-amber-900 mb-1">
              Beskrivelse (valgfri)
            </label>
            <textarea
              v-model="classDesc"
              rows="3"
              placeholder="..."
              class="w-full border border-amber-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-amber-500 resize-none"
            />
          </div>

          <div>
            <label class="block text-sm font-semibold text-amber-900 mb-1">
              Institusjon
            </label>
            <select
              v-model="selectedInstitutionId"
              :disabled="isLoadingInstitutions"
              class="w-full border border-amber-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-amber-500 bg-white"
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
            <p v-if="isLoadingInstitutions" class="text-amber-700 text-xs mt-1">
              Laster institusjoner...
            </p>
            <p v-else-if="institutions.length === 0" class="text-red-600 text-xs mt-1">
              Ingen institusjoner funnet. Kontakt administrator.
            </p>
            <p v-if="fieldErrors.institutionId" class="text-red-600 text-xs mt-1">
              {{ fieldErrors.institutionId }}
            </p>
          </div>

          <label class="flex items-center gap-3 cursor-pointer">
            <input v-model="isGameEnabled" type="checkbox" class="w-5 h-5 accent-amber-700" />
            <span class="text-sm font-semibold text-amber-900">Aktiver spill for klassen</span>
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
