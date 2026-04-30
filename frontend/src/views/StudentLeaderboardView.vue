<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getClassroomsLeaderboardByInstitutionOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'
import { useStudentStore } from '@/stores/StudentStore'

const studentStore = useStudentStore()
const institutionId = computed(() => studentStore.institutionId)

const { data, isLoading, isError } = useQuery(
  institutionId.value
    ? getClassroomsLeaderboardByInstitutionOptions({
        path: { institutionId: institutionId.value },
      })
    : { queryKey: ['leaderboard-disabled'] as const, queryFn: () => null, enabled: false },
)

const entries = computed(() => (data.value as { data?: unknown[] } | null)?.data ?? [])
</script>

<template>
  <SceneComponent title="Ledertavle" previous="/student-home" background="/StartBackground.jpg">
    <div class="flex justify-center p-4 flex-1">
      <div class="w-full max-w-2xl bg-white/90 rounded-2xl p-6 flex flex-col gap-4">
        <h1 class="text-center text-xl font-bold">Klasserom ledertavle</h1>

        <div v-if="isLoading" class="text-center text-gray-500 py-8">Laster...</div>

        <div v-else-if="isError" class="text-center text-red-500 py-8">
          Kunne ikke hente ledertavlen.
        </div>

        <table v-else class="w-full border-collapse">
          <thead>
            <tr class="bg-gray-800 text-white">
              <th class="px-4 py-3 text-left rounded-tl-lg">Plassering</th>
              <th class="px-4 py-3 text-left">Klasse</th>
              <th class="px-4 py-3 text-right rounded-tr-lg">Poeng</th>
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
  </SceneComponent>
</template>

<style scoped>
.leaderboard-container {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.leaderboard-card {
  width: 100%;
  max-width: 800px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 2rem;
}

.leaderboard-card h1 {
  text-align: center;
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
}

.student-leaderboard-table {
  width: 100%;
  border-collapse: collapse;
}

.leaderboard-table th,
.leaderboard-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.leaderboard-table thead {
  background-color: #2c3e50;
  color: white;
}
</style>
