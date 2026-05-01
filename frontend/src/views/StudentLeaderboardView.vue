<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getClassroomsLeaderboardByInstitutionOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'
import { useStudentStore } from '@/stores/StudentStore'

const studentStore = useStudentStore()
const institutionId = computed(() => studentStore.institutionId)

const queryOptions = computed(() =>
  institutionId.value
    ? getClassroomsLeaderboardByInstitutionOptions({ path: { institutionId: institutionId.value } })
    : { queryKey: ['leaderboard-disabled'], queryFn: () => null, enabled: false } as any,
)

const { data, isLoading, isError } = useQuery(queryOptions)

const entries = computed(() => (data.value as any)?.data ?? [])
</script>

<template>
  <SceneComponent title="Ledertavle" previous="/student-home" background="/StartBackground.jpg">
    <div class="flex justify-center items-start p-4 flex-1">
      <div class="w-full max-w-2xl bg-white/90 rounded-md p-6 flex flex-col gap-4">
        <h1 class="text-center text-xl font-bold">Klasserom ledertavle</h1>

        <div v-if="isLoading" class="text-center text-gray-500 py-8">Laster...</div>

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
                class="border-b border-gray-200 last:border-0"
              >
                <td class="px-4 py-3 font-bold text-gray-700">#{{ entry.rank }}</td>
                <td class="px-4 py-3 text-gray-900">{{ entry.title }}</td>
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
