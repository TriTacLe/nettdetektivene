<script setup lang="ts">
import { ref } from 'vue'
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
  onError: (e: any) => {
    fieldError.value = e.message ?? 'Kunne ikke lagre profilen'
  },
})

function handleSubmit() {
  fieldError.value = ''
  if (!displayName.value.trim()) {
    fieldError.value = 'Brukernavn kreves'
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
      class="flex flex-col flex-1 items-center px-4 lg:px-12 pt-4 pb-6 overflow-y-auto lg:overflow-hidden"
    >
      <CaseFile
        tab-label="LAG DIN DETEKTIV"
        class="w-full max-w-[64rem] lg:max-h-[78vh] lg:min-h-0"
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
                class="w-full rounded-xl px-3 py-2 text-base text-stone-900 bg-amber-50 border-2 border-stone-400 focus:outline-none focus:border-amber-700"
                @keyup.enter="handleSubmit"
              />
              <span v-if="fieldError" class="text-red-600 text-xs font-semibold">
                {{ fieldError }}
              </span>
            </div>

            <div class="flex-1 flex items-center justify-center min-h-0 overflow-hidden">
              <AvatarPreview :config="avatarConfig" :size="200" :lazy-load="false" />
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
            <AvatarEditor v-model="avatarConfig" />
          </div>
        </template>
      </CaseFile>
    </div>
  </SceneComponent>
</template>
