<script setup lang="ts">
import { getAllClassroomsOptions, getClassroomsLeaderboardByInstitutionOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'

const router = useRouter()

const { data: classroomsData, isLoading: classroomsLoading } = useQuery(getAllClassroomsOptions())

const institutionId = computed(() => classroomsData.value?.data?.[0]?.institutionId ?? null)

const { data, isLoading: leaderboardLoading, isError } = useQuery(
  computed(() => {
    if (!institutionId.value) {
      return {
        queryKey: ['-'] as const,
        queryFn: () => Promise.resolve({ data: [] }),
        enabled: false,
        staleTime: Infinity,
        refetchOnWindowFocus: false
      }
    }
    const baseOpts = getClassroomsLeaderboardByInstitutionOptions({ path: { institutionId: institutionId.value } })
    return {
      ...baseOpts,
      enabled: true
    } as any
  })
)

const isLoading = computed(() => classroomsLoading.value || leaderboardLoading.value)
const entries = computed(() => (data.value as { data?: Array<{ classroomId: string; rank: number; title: string; totalScore: number }> } | null)?.data ?? [])
</script>

<template>
  <SceneComponent title="Ledertavle" previous="/teacher-home" background="/StartBackground.jpg">
    <div class="flex justify-center items-start p-4 flex-1">
      <div class="w-full max-w-2xl bg-white/90 rounded-md p-6 flex flex-col gap-4">
        <h1 class="text-center text-xl font-bold">Klasseromrangering</h1>

        <div v-if="isLoading" class="text-center text-gray-500 py-8">
          Laster...
        </div>

        <div v-else-if="isError" class="text-center text-red-500 py-8">
          Kunne ikke hente ledertavlen.
        </div>

        <div v-else class="leaderboard-scroll">
          <table class="w-full border-collapse">
            <thead>
              <tr class="bg-gray-800 text-white">
                <th class="px-4 py-3 text-left sticky top-0 bg-gray-800 z-10">Plassering</th>
                <th class="px-4 py-3 text-left sticky top-0 bg-gray-800 z-10">Klasse</th>
                <th class="px-4 py-3 text-right sticky top-0 bg-gray-800 z-10">Poeng</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="entry in entries"
                :key="entry.classroomId"
                class="border-b border-gray-200 last:border-0 cursor-pointer hover:bg-indigo-50 transition"
                @click="
                  router.push({
                    name: 'teacher-classroom-leaderboard',
                    params: { classroomId: entry.classroomId },
                  })
                "
              >
                <td class="px-4 py-3 font-bold text-gray-700">#{{ entry.rank }}</td>
                <td class="px-4 py-3 text-gray-900 font-medium">
                  {{ entry.title }}
                  <span class="ml-2 text-xs text-indigo-500 font-normal">Trykk for detaljer →</span>
                </td>
                <td class="px-4 py-3 text-right font-semibold text-indigo-700">
                  {{ entry.totalScore }}
                </td>
              </tr>
              <tr v-if="entries.length === 0">
                <td colspan="3" class="px-4 py-8 text-center text-gray-400">
                  Ingen resultater ennå.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.leaderboard-scroll {
  max-height: 60vh;
  overflow-y: auto;
  border: 1px solid rgba(31, 41, 55, 0.15);
  border-radius: 0.25rem;
  background: #ffffff;
}

.leaderboard-scroll::-webkit-scrollbar {
  width: 6px;
}

.leaderboard-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.leaderboard-scroll::-webkit-scrollbar-thumb {
  background: rgba(31, 41, 55, 0.25);
  border-radius: 3px;
}

.leaderboard-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(31, 41, 55, 0.45);
}
</style>
