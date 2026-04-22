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
  onSuccess: () => router.push({ name: 'introduction' }),
  onError: (e: any) => {
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
  <SceneComponent title="Bli med i klasserom" previous="/student-login" next="/introduction">
    <div class="flex flex-col items-center justify-center flex-1 px-6">
      <div class="flex flex-col gap-4 w-full max-w-sm">
        <div class="field">
          <label
            for="join-code"
            class="text-white font-bold text-lg"
            style="-webkit-text-stroke: 1.5px #000; paint-order: stroke fill"
          >
            Klasseromskode
          </label>
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
          :disabled="join.isPending.value"
          class="w-full bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-bold py-3 rounded-xl shadow transition"
          @click="handleJoin"
        >
          {{ join.isPending.value ? 'Blir med...' : 'Bli med' }}
        </button>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.join-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 2rem;
}
</style>
