<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import {
  getClassroomLeaderboardOptions,
  getClassroomOptions,
} from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'

const route = useRoute()
const router = useRouter()
const classroomId = route.params.classroomId as string

const { data: classroomData } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroomTitle = computed(() => classroomData.value?.data?.title ?? 'Klasserom')

const { data, isLoading, isError } = useQuery(
  getClassroomLeaderboardOptions({ path: { classroomId } }),
)
const entries = computed(() => data.value?.data ?? [])
</script>

<template>
  <SceneComponent
    :title="classroomTitle"
    background="/StartBackground.jpg"
    @previous="router.push({ name: 'teacher-leaderboard' })"
  >
    <div class="flex justify-center p-4 flex-1">
      <div class="w-full max-w-2xl bg-white/90 rounded-2xl p-6 flex flex-col gap-4">
        <h1 class="text-center text-xl font-bold">Studentrangering</h1>

        <div v-if="isLoading" class="text-center text-gray-500 py-8">Laster...</div>

        <div v-else-if="isError" class="text-center text-red-500 py-8">
          Kunne ikke hente ledertavlen.
        </div>

        <table v-else class="w-full border-collapse">
          <thead>
            <tr class="bg-gray-800 text-white">
              <th class="px-4 py-3 text-left rounded-tl-lg">Plassering</th>
              <th class="px-4 py-3 text-left">Student</th>
              <th class="px-4 py-3 text-right rounded-tr-lg">Poeng</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="entry in entries"
              :key="entry.userId"
              class="border-b border-gray-200 last:border-0"
            >
              <td class="px-4 py-3 font-bold text-gray-700">#{{ entry.rank }}</td>
              <td class="px-4 py-3 text-gray-900">{{ entry.displayName }}</td>
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
