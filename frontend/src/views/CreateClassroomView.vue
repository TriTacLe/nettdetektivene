<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useMutation } from '@tanstack/vue-query'
import { createClassroomMutation } from '@/generated-client/@tanstack/vue-query.gen'

const router = useRouter()

const title = ref('')
const classDesc = ref('')
const institutionId = ref('')
const isGameEnabled = ref(false)
const serverError = ref<string | null>(null)

interface FieldErrors {
  title?: string
  institutionId?: string
}
const fieldErrors = ref<FieldErrors>({})

function validate(): boolean {
  fieldErrors.value = {}
  if (!title.value.trim()) fieldErrors.value.title = 'Navn på klasserom kreves'
  if (!institutionId.value.trim()) fieldErrors.value.institutionId = 'Institusjons-ID kreves'
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

function submit() {
  serverError.value = null
  if (!validate()) return
  createMutation.mutate({
    body: {
      title: title.value.trim(),
      classDesc: classDesc.value.trim() || undefined,
      institutionId: institutionId.value.trim(),
      gameEnabled: isGameEnabled.value,
    },
  })
}

</script>

<template>
  <SceneComponent
    title="Opprett klasserom"
    background="/StartBackground.jpg"
    previous="classroom-overview"
    next="/classroom/:id"
  >
    <div class="min-h-screen flex flex-col items-center justify-center p-8">
      <div class="bg-amber-50 rounded-2x1 shadow-xl p-8 w-full max-w-md">
        <h1 class="text-2xl font-bold text-amber-900 mb-6">Opprett klasserom</h1>

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
            <label class="block text-sm font-semibold text-amber-900 mb-1"> Institusjons-ID </label>
            <input
              v-model="institutionId"
              type="text"
              placeholder="ID for din skole"
              class="w-full border border-amber-300 rounded-lg px-4 py-2 font-mono focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
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
            :disabled="createMutation.isPending.value"
            class="w-full bg-amber-800 hover:bg-amber-700 disabled:opacity-50 text-white font-bold py-3 rounded-xl shadow transition mt-2"
          >
            {{ createMutation.isPending.value ? 'Oppretter...' : 'Opprett klasserom' }}
          </button>
        </form>
      </div>
    </div>
  </SceneComponent>
</template>
