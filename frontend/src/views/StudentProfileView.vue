<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { getProfileOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'
import CaseFile from '@/components/CaseFile.vue'
import { useAuthStore } from "@/stores/AuthStore.ts"
import { useToast } from '@/composables/UseToast'
import AvatarPreview from '@/components/avatar/AvatarPreview.vue'
import { parseAvatarConfig } from '@/utils/avatar'

const authStore = useAuthStore()
const router = useRouter()
const { showSuccess, showError } = useToast()

const { data, isLoading } = useQuery({ ...getProfileOptions(), refetchInterval: 5000 })
const profile = computed(() => data.value?.data ?? null)
const avatarCfg = computed(() => parseAvatarConfig(profile.value?.avatarConfig ?? null))

const viewportHeight = ref(window.innerHeight)

function onResize() {
  viewportHeight.value = window.innerHeight
}

onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const avatarSize = computed(() => {
  if (viewportHeight.value < 550) return 64
  if (viewportHeight.value < 700) return 88
  return 120
})

const hasPendingNameChange = computed(() => profile.value?.nameChangeStatus === 'PENDING')

watch(
  () => profile.value?.nameChangeStatus,
  (newStatus, oldStatus) => {
    if (oldStatus === 'PENDING' && newStatus && oldStatus !== newStatus) {
      if (newStatus === 'APPROVED') {
        showSuccess('Navneendring godkjent')
      } else if (newStatus === 'REJECTED') {
        showError('Navneendring avvist')
      }
    }
  }
)
</script>

<template>
  <SceneComponent title="Min Profil" previous="/student-home" background="/StartBackground.jpg">
    <div class="profile-scroll-area">
      <div v-if="isLoading" class="text-white text-center py-6">Laster...</div>

      <div v-else-if="profile" class="w-full max-w-sm">
        <CaseFile tab-label="MIN DETEKTIV">
          <template #left>
            <div class="profile-card-inner">
              <AvatarPreview :config="avatarCfg" :size="avatarSize" />

              <button
                class="text-sm font-semibold text-amber-800 underline hover:text-amber-900 transition"
                @click="router.push({ name: 'edit-avatar' })"
              >
                Rediger Detektiv
              </button>

              <div class="w-full flex flex-col profile-stats">
                <div>
                  <p class="text-xs text-stone-600 font-semibold uppercase tracking-wide">Brukernavn</p>
                  <p class="profile-value font-bold text-stone-900">{{ profile.displayName ?? '---' }}</p>
                  <p v-if="hasPendingNameChange" class="text-xs text-amber-700 font-semibold mt-1">
                    Navneendring venter på godkjenning
                  </p>
                </div>

                <div>
                  <p class="text-xs text-stone-600 font-semibold uppercase tracking-wide">Niva</p>
                  <p class="profile-value font-bold text-amber-800">Niva {{ profile.xpLevel ?? 1 }}</p>
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

<style scoped>
.profile-scroll-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  width: 100%;
  padding: 1.5rem 1rem;
  overflow-y: auto;
}

.profile-card-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
}

.profile-stats {
  gap: 1rem;
}

.profile-value {
  font-size: 1.25rem;
}

@media (max-height: 700px) {
  .profile-scroll-area {
    padding: 0.75rem 1rem;
    justify-content: flex-start;
  }

  .profile-card-inner {
    gap: 0.5rem;
    padding: 1rem;
  }

  .profile-stats {
    gap: 0.5rem;
  }

  .profile-value {
    font-size: 1rem;
  }
}

@media (max-height: 550px) {
  .profile-card-inner {
    gap: 0.25rem;
    padding: 0.75rem;
  }

  .profile-stats {
    gap: 0.25rem;
  }
}
</style>