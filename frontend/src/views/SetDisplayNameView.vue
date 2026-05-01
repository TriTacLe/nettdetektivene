<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import CaseFile from '@/components/CaseFile.vue'
import AvatarPreview from '@/components/avatar/AvatarPreview.vue'
import AvatarEditor from '@/components/avatar/AvatarEditor.vue'
import { useMutation } from '@tanstack/vue-query'
import { updateProfileMutation } from '@/generated-client/@tanstack/vue-query.gen.ts'
import { DEFAULT_AVATAR_CONFIG, type AvatarConfig } from '@/types/avatar'
import { serializeAvatarConfig } from '@/utils/avatar'
import { useAuthStore } from '@/stores/AuthStore.ts'

const router = useRouter()
const displayName = ref('')
const fieldError = ref('')
const avatarConfig = ref<AvatarConfig>({ ...DEFAULT_AVATAR_CONFIG })
const authStore = useAuthStore()

const update = useMutation({
  ...updateProfileMutation(),
  onSuccess: async () => {
    await authStore.fetchMe()
    router.push({ name: 'join-classroom' })
  },
  onError: (e: unknown) => {
    const msg = (e as { message?: string })?.message
    fieldError.value = msg && !(e instanceof TypeError) ? msg : 'Kunne ikke lagre profilen. Prøv igjen.'
  },
})

function handleSubmit() {
  fieldError.value = ''
  if (!displayName.value.trim()) {
    fieldError.value = 'Brukernavn kreves'
    nextTick(() => document.getElementById('display-name')?.focus())
    return
  }
  update.mutate({
    body: {
      displayName: displayName.value.trim(),
      avatarConfig: serializeAvatarConfig(avatarConfig.value),
    },
  })
}
</script>

<template>
  <SceneComponent title="Velkommen til Nettdetektivene!" background="/StartBackground.jpg">
    <div
      class="flex flex-col flex-1 items-center px-4 lg:px-12 pt-8 pb-6 overflow-y-auto"
    >
      <CaseFile
        tab-label="LAG DIN DETEKTIV"
        class="w-full max-w-[64rem]"
      >
        <template #left>
          <div class="flex flex-col items-center flex-1 p-3 lg:p-4 gap-2 min-h-0">
            <div class="w-full flex flex-col gap-1">
              <label for="display-name" class="text-sm font-bold text-stone-800">
                Brukernavn
              </label>
              <input
                id="display-name"
                v-model="displayName"
                type="text"
                placeholder="Skriv inn navn"
                maxlength="30"
                class="w-full rounded-xl px-3 py-2 text-base text-stone-900 bg-amber-50 border-2 border-stone-400 focus:border-amber-700 focus-visible:outline-2 focus-visible:outline-amber-700 focus-visible:outline-offset-2"
                @keyup.enter="handleSubmit"
              />
              <p v-if="fieldError" class="profile-save-error" role="alert">
                {{ fieldError }}
              </p>
            </div>

            <div class="flex items-center justify-center py-6">
              <AvatarPreview :config="avatarConfig" :size="280" :lazy-load="false" />
            </div>

            <button
              :disabled="update.isPending.value"
              class="w-full bg-amber-800 hover:bg-amber-700 disabled:opacity-50 text-white font-bold py-3 rounded-xl border-2 border-stone-700 shadow-md transition"
              @click="handleSubmit"
            >
              {{ update.isPending.value ? 'Lagrer...' : 'Fortsett' }}
            </button>
          </div>
        </template>
        <template #right>
          <div class="p-3 pt-9 min-h-0 overflow-visible lg:overflow-hidden">
            <AvatarEditor v-model="avatarConfig" :hide-categories="['prop']" />
          </div>
        </template>
      </CaseFile>
    </div>
  </SceneComponent>
</template>

