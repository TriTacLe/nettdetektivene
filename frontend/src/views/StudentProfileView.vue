<script setup lang="ts">
import { computed, ref } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { getProfileOptions, getProfileQueryKey, updateProfileMutation } from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'
import {useAuthStore} from "@/stores/AuthStore.ts";

const authStore = useAuthStore()
const queryClient = useQueryClient()
const { data, isLoading } = useQuery(getProfileOptions())
const profile = computed(() => data.value?.data ?? null)

// Edit mode state
const isEditingName = ref(false)
const newDisplayName = ref('')
const updateError = ref<string | null>(null)

// Update profile mutation
const updateProfile = useMutation({
  ...updateProfileMutation(),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: getProfileQueryKey() })
    queryClient.invalidateQueries({ queryKey: ['getUser'] })
    isEditingName.value = false
    updateError.value = null
    // Refresh auth store user data
    authStore.fetchMe()
  },
  onError: (error: any) => {
    updateError.value = error.message || 'Kunne ikke oppdatere navn'
  }
})

function startEditingName() {
  newDisplayName.value = profile.value?.displayName || ''
  isEditingName.value = true
  updateError.value = null
}

function cancelEditingName() {
  isEditingName.value = false
  updateError.value = null
}

function saveDisplayName() {
  if (!newDisplayName.value.trim()) {
    updateError.value = 'Navn kan ikke være tomt'
    return
  }
  updateProfile.mutate({
    body: {
      displayName: newDisplayName.value.trim()
    }
  })
}
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
          <!-- Display Name Section -->
          <div>
            <div class="flex items-center justify-between">
              <p class="text-xs text-gray-500 font-semibold uppercase tracking-wide">Brukernavn</p>
              <button
                v-if="!isEditingName"
                @click="startEditingName"
                class="text-xs text-indigo-600 hover:text-indigo-800 font-medium"
              >
                Endre
              </button>
            </div>
            
            <!-- View Mode -->
            <p v-if="!isEditingName" class="text-xl font-bold text-gray-900">{{ profile.displayName ?? '—' }}</p>
            
            <!-- Edit Mode -->
            <div v-else class="mt-2 flex flex-col gap-2">
              <input
                v-model="newDisplayName"
                type="text"
                placeholder="Skriv nytt navn"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                @keyup.enter="saveDisplayName"
              />
              <p v-if="updateError" class="text-red-600 text-xs">{{ updateError }}</p>
              <p v-if="updateProfile.isPending.value" class="text-gray-500 text-xs">Lagrer...</p>
              <div class="flex gap-2">
                <button
                  @click="saveDisplayName"
                  :disabled="updateProfile.isPending.value"
                  class="flex-1 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-sm font-bold py-2 rounded-lg transition"
                >
                  Lagre
                </button>
                <button
                  @click="cancelEditingName"
                  :disabled="updateProfile.isPending.value"
                  class="flex-1 bg-gray-200 hover:bg-gray-300 disabled:opacity-50 text-gray-700 text-sm font-bold py-2 rounded-lg transition"
                >
                  Avbryt
                </button>
              </div>
              <p class="text-xs text-amber-600 italic">
                Tips: Hvis du er medlem av et klasserom, må læreren godkjenne navneendringen.
              </p>
            </div>
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

        <button
          class="w-full bg-red-600 hover:bg-red-500 text-white font-bold py-3 rounded-2x1 shadow transition"
          @click="authStore.logout()"
        >
          Logg ut
        </button>
      </div>
    </div>
  </SceneComponent>
</template>
