<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useMutation } from '@tanstack/vue-query'
import { updateUserMutation } from '@/generated-client/@tanstack/vue-query.gen.ts'

const router = useRouter()
const displayName = ref('')
const fieldError = ref('')

const update = useMutation({
  ...updateUserMutation(),
  onSuccess: () => router.push({ name: 'join-classroom' }),
  onError: (e: any) => {
    fieldError.value = e.message ?? 'Kunne ikke lagre brukernavn'
  },
})

function handleSubmit() {
  fieldError.value = ''
  if (!displayName.value.trim()) {
    fieldError.value = 'Brukernavn kreves'
  }
}
</script>

<template>
  <SceneComponent title="Velg brukernavn" background="/StartBackground.jpg">
    <div class="flex flex-col items-center justify-center flex-1 px-6">
      <div class="flex flex-col gap-4 w-full max-w-sm">
        <p class="text-white font-semibold text-center text-sm"
           style="-webkit-text-stroke: 1px #000; paint-order: stroke fill;">
          Dette er navnet som vises til læreren og medelever.
        </p>

        <div class="flex flex-col gap-1">
          <label for="display-name" class="text-white font-bold text-lg"
                 style="-webkit-text-stroke: 1.5px #000; paint-order: stroke fill;">
            Brukernavn
          </label>
          <input
            id="display-name"
            v-model="displayName"
            type="text"
            placeholder="Skriv inn navn"
            maxlength="30"
            class="w-full rounded-xl px-4 py-3 text-lg border-2 border-gray-300 focus:outline-none focus:border-indigo-500"
            @keyup.enter="handleSubmit"
          />
          <span v-if="fieldError" class="text-red-300 text-sm font-semibold bg-black/50 px-2 py-1 rounded">
            {{ fieldError }}
          </span>
        </div>

        <button
          :disabled="update.isPending.value"
          class="w-full bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-bold py-3 rounded-xl shadow transition"
          @click="handleSubmit"
        >
          {{ update.isPending.value ? 'Lagrer...' : 'Fortsett' }}
        </button>
      </div>
    </div>
  </SceneComponent>
</template>
