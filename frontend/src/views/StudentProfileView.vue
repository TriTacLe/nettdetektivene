<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { getProfileOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'
import CaseFile from '@/components/CaseFile.vue'
import { useAuthStore } from "@/stores/AuthStore.ts"
import AvatarPreview from '@/components/avatar/AvatarPreview.vue'
import { parseAvatarConfig } from '@/utils/avatar'

const authStore = useAuthStore()
const router = useRouter()
const { data, isLoading } = useQuery(getProfileOptions())
const profile = computed(() => data.value?.data ?? null)
const avatarCfg = computed(() => parseAvatarConfig(profile.value?.avatarConfig ?? null))
</script>

<template>
  <SceneComponent title="Min Profil" background="/StartBackground.jpg">
    <div class="flex flex-col items-center flex-1 w-full px-4 lg:px-6 pb-10 pt-6">
      <div v-if="isLoading" class="text-white text-center py-6">Laster...</div>

      <div v-else-if="profile" class="w-full max-w-sm">
        <CaseFile tab-label="MIN DETEKTIV">
          <template #left>
            <div class="flex flex-col items-center gap-4 p-6">
              <AvatarPreview :config="avatarCfg" :size="120" />
              <button
                class="text-sm font-semibold text-amber-800 underline hover:text-amber-900 transition"
                @click="router.push({ name: 'edit-avatar' })"
              >
                Rediger Detektiv
              </button>

              <div class="w-full flex flex-col gap-4">
                <div>
                  <p class="text-xs text-stone-600 font-semibold uppercase tracking-wide">Brukernavn</p>
                  <p class="text-xl font-bold text-stone-900">{{ profile.displayName ?? '---' }}</p>
                </div>

                <div>
                  <p class="text-xs text-stone-600 font-semibold uppercase tracking-wide">Niva</p>
                  <p class="text-xl font-bold text-amber-800">Niva {{ profile.xpLevel ?? 1 }}</p>
                </div>

                <div>
                  <p class="text-xs text-stone-600 font-semibold uppercase tracking-wide">XP</p>
                  <div class="flex items-center gap-3 mt-1">
                    <div class="flex-1 bg-stone-300 rounded-full h-3">
                      <div
                        class="bg-amber-700 h-3 rounded-full transition-all"
                        :style="{ width: `${Math.min((profile.xp ?? 0) % 100, 100)}%` }"
                      />
                    </div>
                    <span class="text-sm font-bold text-stone-800">{{ profile.xp ?? 0 }} XP</span>
                  </div>
                </div>
              </div>

              <button
                class="w-full bg-red-800 hover:bg-red-700 text-white font-bold py-3 rounded-xl border-2 border-stone-700 shadow-md transition"
                @click="authStore.logout()"
              >
                Logg ut
              </button>
            </div>
          </template>
        </CaseFile>
      </div>
    </div>
  </SceneComponent>
</template>
