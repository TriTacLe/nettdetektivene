<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import { useMutation } from '@tanstack/vue-query'
import { joinMutation } from '@/generated-client/@tanstack/vue-query.gen.ts'

const router = useRouter()
const joinCode = ref('')
const fieldError = ref('')

const join = useMutation({
  ...joinMutation(),
  onSuccess: () => {
    router.push({ name: 'awaiting-approval' })
  },
  onError: (e: Error) => {
    fieldError.value = e.message ?? 'Kunne ikke bli med i klasserommet'
  },
})

function handleJoin() {
  fieldError.value = ''
  if (!joinCode.value.trim()) {
    fieldError.value = 'Klasseromskode kreves'
    return
  }
  join.mutate({ body: { joinCode: joinCode.value.trim() } })
}
</script>

<template>
  <SceneComponent title="Bli med i klasserom">
    <div class="flex flex-col items-center justify-center flex-1 px-6">
      <div class="flex flex-col gap-4 w-full max-w-sm">
        <div class="field">
          <label for="join-code">Klasseromskode</label>
          <input
            id="join-code"
            v-model="joinCode"
            type="text"
            placeholder="Skriv inn koden"
            class="w-full rounded-xl px-4 py-3 text-lg font-mono border-2 border-gray-300 focus:outline-none focus:border-indigo-500"
            @keyup.enter="handleJoin"
          />
          <span
            v-if="fieldError"
            class="text-red-300 text-sm font-semibold bg-black/50 px-2 py-1 rounded"
          >
            {{ fieldError }}
          </span>
        </div>

        <button
          class="btn-vn"
          :disabled="join.isPending.value"
          @click="handleJoin"
        >
          {{ join.isPending.value ? 'Blir med...' : 'Bli med' }}
        </button>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
label {
  color: white;
  font-weight: 700;
  font-size: 1.25rem;
  -webkit-text-stroke: 2px #000000;
  paint-order: stroke fill;
}
</style>
