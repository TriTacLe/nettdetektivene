<script setup lang="ts">
import { ref, computed, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation } from '@tanstack/vue-query'
import { getProfileOptions, updateProfileMutation } from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'
import CaseFile from '@/components/CaseFile.vue'
import AvatarPreview from '@/components/avatar/AvatarPreview.vue'
import AvatarEditor from '@/components/avatar/AvatarEditor.vue'
import { parseAvatarConfig, serializeAvatarConfig } from '@/utils/avatar'
import { DEFAULT_AVATAR_CONFIG, type AvatarConfig } from '@/types/avatar'

const router = useRouter()

const { data, isLoading } = useQuery(getProfileOptions())
const avatarConfig = ref<AvatarConfig>({ ...DEFAULT_AVATAR_CONFIG })
const displayName = ref('')
const originalDisplayName = ref('')

const nameChangeStatus = computed(() => data.value?.data?.nameChangeStatus ?? null)
const hasPendingNameChange = computed(() => nameChangeStatus.value === 'PENDING')

watchEffect(() => {
  const profile = data.value?.data
  if (profile) {
    avatarConfig.value = parseAvatarConfig(profile.avatarConfig ?? null)
    displayName.value = profile.displayName ?? ''
    originalDisplayName.value = profile.displayName ?? ''
  }
})

const saveError = ref('')

const save = useMutation({
  ...updateProfileMutation(),
  onSuccess: () => router.push({ name: 'student-profile' }),
  onError: (e: any) => {
    saveError.value = e.message ?? 'Kunne ikke lagre profilen'
  },
})

function handleSave() {
  saveError.value = ''
  if (!displayName.value.trim()) {
    saveError.value = 'Brukernavn kreves'
    return
  }

  const trimmed = displayName.value.trim()
  if (trimmed !== originalDisplayName.value && hasPendingNameChange.value) {
    saveError.value = 'Du har allerede en navneendring til godkjenning. Vennligst vent på svar fra læreren din.'
    return
  }

  const body: Record<string, string> = {
    avatarConfig: serializeAvatarConfig(avatarConfig.value),
  }

  if (trimmed !== originalDisplayName.value) {
    body.displayName = trimmed
  }

  save.mutate({ body })
}
</script>

<template>
  <SceneComponent title="Rediger profil" background="/StartBackground.jpg">
    <div class="flex flex-col flex-1 items-center px-4 lg:px-12 pt-4 pb-6 overflow-y-auto lg:overflow-hidden">
      <div v-if="isLoading" class="text-white text-center py-6">Laster...</div>

      <CaseFile v-else tab-label="REDIGER PROFIL" class="w-full max-w-[64rem] lg:max-h-[78vh] lg:min-h-0">
        <template #left>
          <div class="flex flex-col items-center flex-1 p-3 lg:p-4 gap-2 min-h-0">

            <div
              v-if="hasPendingNameChange"
              class="w-full px-4 py-3 rounded-xl bg-amber-100 border-2 border-amber-400 text-amber-900 text-sm font-semibold text-center"
            >
              Du har spurt om navneendring. Vennligst vent til læreren godkjenner.
            </div>

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
                :disabled="hasPendingNameChange"
                class="w-full rounded-xl px-3 py-2 text-base text-stone-900 bg-amber-50 border-2 border-stone-400 focus:outline-none focus:border-amber-700 disabled:opacity-50 disabled:cursor-not-allowed"
                @keyup.enter="handleSave"
              />
              <span v-if="saveError" class="text-red-600 text-xs font-semibold">
                {{ saveError }}
              </span>
            </div>

            <div class="flex-1 flex items-center justify-center min-h-0 overflow-hidden">
              <AvatarPreview :config="avatarConfig" :size="200" />
            </div>

            <div class="flex gap-3 w-full">
              <button
                class="flex-1 bg-stone-300 hover:bg-stone-400 text-stone-800 font-bold py-3 rounded-xl border-2 border-stone-700 shadow-md transition"
                @click="router.push({ name: 'student-profile' })"
              >
                Avbryt
              </button>
              <button
                :disabled="save.isPending.value"
                class="flex-1 bg-amber-800 hover:bg-amber-700 disabled:opacity-50 text-white font-bold py-3 rounded-xl border-2 border-stone-700 shadow-md transition"
                @click="handleSave"
              >
                {{ save.isPending.value ? 'Lagrer...' : 'Lagre' }}
              </button>
            </div>
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
