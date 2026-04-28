<script setup lang="ts">
import { getMyClassrooms } from '@/generated-client'
import { getClassroom } from '@/generated-client'
import { useQuery } from '@tanstack/vue-query'
import { getClassroomsLeaderboardByInstitutionOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import { ref, computed, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'

const router = useRouter()
const institutionId = ref<string | null>(null)

const { data: myClassrooms } = useQuery({
  queryKey: ['my-classrooms'],
  queryFn: async () => {
    const { data } = await getMyClassrooms()
    return data?.data ?? []
  },
})

const firstClassroomId = computed(() => myClassrooms.value?.[0]?.classroomId)

watchEffect(async () => {
  if (firstClassroomId.value && !institutionId.value) {
    const res = await getClassroom({ path: { id: firstClassroomId.value } })
    institutionId.value = res.data?.data?.institutionId ?? null
  }
})

const { data, isLoading, isError } = useQuery(
  institutionId.value
    ? getClassroomsLeaderboardByInstitutionOptions({
        path: { institutionId: institutionId.value },
      })
    : { enabled: false } as any,
)

const entries = computed(() => (data.value as any)?.data ?? [])
</script>

<template>
  <SceneComponent title="Ledertavle" previous="/teacher-home" background="/StartBackground.jpg">
    <div class="flex justify-center p-4 flex-1">
      <div class="w-full max-w-2xl bg-white/90 rounded-2xl p-6 flex flex-col gap-4">
        <h1 class="text-center text-xl font-bold">Klasseromrangering</h1>

        <div v-if="isLoading || !institutionId" class="text-center text-gray-500 py-8">
          Laster...
        </div>

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
  </SceneComponent>
</template>
