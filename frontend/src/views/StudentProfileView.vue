<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getProfileOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'

const { data, isLoading } = useQuery(getProfileOptions())
const profile = computed(() => data.value?.data ?? null)
</script>

<template>
  <SceneComponent title="Min profil" background="/StartBackground.jpg">
    <div class="flex flex-col items-center flex-1 w-full px-6 pb-10 pt-6">
      <div v-if="isLoading" class="text-white text-center py-6">Laster...</div>

      <div v-else-if="profile" class="flex flex-col items-center gap-6 w-full max-w-sm">
        <div
          class="w-24 h-24 rounded-full bg-indigo-200 flex items-center justify-center shadow-lg text-4xl"
        >
          Avatar
        </div>

        <div class="bg-white rounded-2xl shadow w-full px-6 py-5 flex flex-col gap-4">
          <div>
            <p class="text-xs text-gray-500 font-semibold uppercase tracking-wide">Brukernavn</p>
            <p class="text-xl font-bold text-gray-900">{{ profile.displayName ?? '—' }}</p>
          </div>

          <div>
            <p class="text-xs text-gray-500 font-semibold uppercase tracking-wide">Nivå</p>
            <p class="text-xl font-bold text-indigo-700">Nivå {{ profile.xpLevel ?? 1 }}</p>
          </div>

          <div>
            <p class="text-xs text-gray-500 font-semibold uppercase tracking-wide">XP</p>
            <div class="flex items-center gap-3 mt-1">
              <div class="flex-1 bg-gray-200 rounded-full h-3">
                <div
                  class="bg-indigo-500 h-3 rounded-full transition-all"
                  :style="{ width: `${Math.min((profile.xp ?? 0) % 100, 100)}%` }"
                />
              </div>
              <span class="text-sm font-bold text-gray-700">{{ profile.xp ?? 0 }} XP</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>
