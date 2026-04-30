<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery } from '@tanstack/vue-query'
import {
  getClassroomLeaderboardOptions,
  getStudentScoreOptions,
  getClassroomTasksOptions,
  getAllNotebooksOptions,
  getStudentsOptions,
  getAchievementsForStudentOptions,
  getAllAchievementsOptions,
} from '@/generated-client/@tanstack/vue-query.gen.ts'


const route = useRoute()
const classroomId = route.params.id as string
const userId = route.params.userId as string

// Canonical stop order and display labels from vision doc
const STOP_ORDER = [
  'nyhetskvartalet',
  'fotografen',
  'postkontoret',
  'passordbanken',
  'markedsplassen',
  'sosialen',
  'datasenteret',
]

const STOP_LABELS: Record<string, string> = {
  nyhetskvartalet: 'Nyhetskvartalet',
  fotografen: 'Fotografen',
  postkontoret: 'Postkontoret',
  passordbanken: 'Passordbanken',
  markedsplassen: 'Markedsplassen',
  sosialen: 'Den sosiale møteplassen',
  datasenteret: 'Datasenteret',
}

const openNotebookStop = ref<string | null>(null)

const { data: leaderboardData, isLoading: loadingLeaderboard } = useQuery(
  getClassroomLeaderboardOptions({ path: { classroomId } }),
)

const { data: studentsData, isLoading: loadingStudents } = useQuery(
  getStudentsOptions({ path: { classroomId } }),
)

const { data: scoreData, isLoading: loadingScores } = useQuery(
  getStudentScoreOptions({ path: { classroomId, userId } }),
)

// Classroom-scoped task list. Using getAllTasks() here was the source of the
// progress-percentage bug: it returned every task in the system, so stops
// where other classrooms had also created tasks (e.g. Fotografen) showed an
// inflated denominator and a low percentage even for a fully-finished student.
const { data: tasksData, isLoading: loadingTasks } = useQuery(
  getClassroomTasksOptions({ path: { classroomId }, query: { activeOnly: true } }),
)

const { data: notebookData, isLoading: loadingNotebooks } = useQuery(
  getAllNotebooksOptions({ path: { classroomId } }),
)

const { data: achievementsData, isLoading: loadingAchievements } = useQuery(
  getAchievementsForStudentOptions({ path: { userId } }),
)

const { data: allAchievementsData, isLoading: loadingAllAchievements } = useQuery(
  getAllAchievementsOptions(),
)

const isLoading = computed(
  () =>
    loadingLeaderboard.value ||
    loadingScores.value ||
    loadingTasks.value ||
    loadingNotebooks.value ||
    loadingStudents.value ||
    loadingAchievements.value ||
    loadingAllAchievements.value,
)

const studentEntry = computed(() =>
  leaderboardData.value?.data?.find((e) => e.userId === userId),
)

const studentRecord = computed(() =>
  studentsData.value?.data?.find((s) => s.userId === userId),
)

const displayName = computed(
  () => studentRecord.value?.displayName ?? studentEntry.value?.displayName ?? 'Ukjent elev',
)

const taskScores = computed(() => scoreData.value?.data ?? [])

const allTasks = computed(() => {
  const out: NonNullable<NonNullable<typeof tasksData.value>['data']>[number]['task'][] = []
  for (const ct of tasksData.value?.data ?? []) {
    if (ct.isActive && ct.task) out.push(ct.task)
  }
  return out.filter((t): t is NonNullable<typeof t> => t !== undefined)
})

const scoreMap = computed(() => {
  const map = new Map<string, (typeof taskScores.value)[0]>()
  for (const score of taskScores.value) {
    if (score.taskId) map.set(score.taskId, score)
  }
  return map
})

const studentNotebooks = computed(() =>
  (notebookData.value?.data ?? []).filter((n) => n.userId === userId),
)

const earnedAchievements = computed(() => achievementsData.value?.data ?? [])
const allAchievements = computed(() => allAchievementsData.value?.data ?? [])

const earnedIds = computed(() => new Set(earnedAchievements.value.map((a) => a.achievementId)))

const notebookByStop = computed(() => {
  const map = new Map<string, (typeof studentNotebooks.value)[0]>()
  for (const entry of studentNotebooks.value) {
    if (entry.stopLocation) map.set(entry.stopLocation, entry)
  }
  return map
})

const totalActive = computed(() => allTasks.value.length)

const totalPassed = computed(() => taskScores.value.filter((s) => s.firstPassedAt != null).length)

const completionPct = computed(() =>
  totalActive.value > 0 ? Math.round((totalPassed.value / totalActive.value) * 100) : 0,
)

const xpLevel = computed(() => studentEntry.value?.xpLevel ?? 1)
const xpPoints = computed(() => studentEntry.value?.xp ?? 0)
const totalScore = computed(() => studentEntry.value?.totalScore ?? 0)
const joinedAt = computed(() => {
  const iso = studentRecord.value?.joinedAt
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('nb-NO')
})

type StopStatus = 'fullfort' | 'pagar' | 'last'

const stopData = computed(() => {
  // collect unique stop locations present in active tasks
  const stopSet = new Set(allTasks.value.map((t) => t.stopLocation).filter(Boolean) as string[])

  // order by canonical list, then append any unknown stops at the end
  const orderedKeys = [
    ...STOP_ORDER.filter((k) => stopSet.has(k)),
    ...[...stopSet].filter((k) => !STOP_ORDER.includes(k)).sort(),
  ]

  return orderedKeys.map((stopKey) => {
    const stopTasks = allTasks.value.filter((t) => t.stopLocation === stopKey)
    const stopTaskIds = new Set(stopTasks.map((t) => t.id).filter(Boolean) as string[])
    const passed = taskScores.value.filter(
      (s) => s.taskId && stopTaskIds.has(s.taskId) && s.firstPassedAt != null,
    ).length
    const pct = stopTasks.length > 0 ? Math.round((passed / stopTasks.length) * 100) : 0
    const status: StopStatus = pct === 100 ? 'fullfort' : passed > 0 ? 'pagar' : 'last'
    return {
      key: stopKey,
      label: STOP_LABELS[stopKey] ?? stopKey.charAt(0).toUpperCase() + stopKey.slice(1),
      total: stopTasks.length,
      passed,
      pct,
      status,
      tasks: stopTasks.map((task) => ({
        task,
        score: task.id ? scoreMap.value.get(task.id) : undefined,
      })),
      notebook: notebookByStop.value.get(stopKey),
    }
  })
})

function toggleNotebook(stopKey: string) {
  openNotebookStop.value = openNotebookStop.value === stopKey ? null : stopKey
}

function formatDate(iso: string | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('nb-NO')
}
</script>

<template>
  <SceneComponent
    title="Elev Progresjon"
    :previous="`/classroom/${classroomId}`"
    background="/StartBackground.jpg"
  >
    <div v-if="isLoading" class="text-center text-white py-12">Laster...</div>

    <div v-else class="flex flex-col items-center w-full px-4 sm:px-6 pb-10 pt-4 gap-4">
      <!-- Student name -->
      <div class="w-full max-w-2xl">
        <h2 class="text-2xl font-black text-white text-center drop-shadow">{{ displayName }}</h2>
      </div>

      <!-- Stat boxes: 2 rows × 3 columns -->
      <div class="w-full max-w-2xl grid grid-cols-3 gap-3">
        <div class="bg-white rounded-2xl shadow p-4 flex flex-col items-center gap-1">
          <p class="text-3xl font-black text-blue-600">{{ xpLevel }}</p>
          <p class="text-xs font-semibold text-gray-500 text-center">Nivå</p>
        </div>
        <div class="bg-white rounded-2xl shadow p-4 flex flex-col items-center gap-1">
          <p class="text-3xl font-black text-amber-500">{{ earnedAchievements.length }}</p>
          <p class="text-xs font-semibold text-gray-500 text-center">Medaljer</p>
        </div>
        <div class="bg-white rounded-2xl shadow p-4 flex flex-col items-center gap-1">
          <p class="text-3xl font-black text-teal-500">{{ completionPct }}%</p>
          <p class="text-xs font-semibold text-gray-500 text-center">Fullført</p>
        </div>
        <div class="bg-white rounded-2xl shadow p-4 flex flex-col items-center gap-1">
          <p class="text-2xl font-black text-purple-600">{{ xpPoints }}</p>
          <p class="text-xs font-semibold text-gray-500 text-center">XP</p>
        </div>
        <div class="bg-white rounded-2xl shadow p-4 flex flex-col items-center gap-1">
          <p class="text-2xl font-black text-gray-700">{{ totalScore }}</p>
          <p class="text-xs font-semibold text-gray-500 text-center">Totalpoeng</p>
        </div>
        <div class="bg-white rounded-2xl shadow p-4 flex flex-col items-center gap-1">
          <p class="text-lg font-black text-gray-500 text-center leading-tight">{{ joinedAt }}</p>
          <p class="text-xs font-semibold text-gray-500 text-center">Ble med</p>
        </div>
      </div>

      <!-- Achievements: unlocked + locked -->
      <div class="w-full max-w-2xl bg-white rounded-2xl shadow p-5">
        <h3 class="font-bold text-gray-900 text-lg mb-3">
          Prestasjoner
          <span class="text-sm font-normal text-gray-500 ml-1">
            ({{ earnedAchievements.length }}/{{ allAchievements.length }})
          </span>
        </h3>
        <div v-if="allAchievements.length === 0" class="text-gray-500 text-sm italic">
          Ingen prestasjoner er opprettet ennå.
        </div>
        <div v-else class="flex flex-wrap gap-2">
          <div
            v-for="ach in allAchievements"
            :key="String(ach.id)"
            class="px-3 py-1.5 rounded-full text-sm font-semibold flex items-center gap-2"
            :class="
              earnedIds.has(ach.id)
                ? 'bg-amber-100 text-amber-800'
                : 'bg-gray-100 text-gray-400'
            "
          >
            <span>{{ earnedIds.has(ach.id) ? '🏆' : '🔒' }}</span>
            <span>{{ ach.title }}</span>
          </div>
        </div>
      </div>

      <!-- No active tasks -->
      <div
        v-if="stopData.length === 0"
        class="w-full max-w-2xl bg-white rounded-2xl shadow p-5 text-center text-gray-500 text-sm"
      >
        Ingen aktive oppgaver er lagt til enda.
      </div>

      <!-- Stop cards -->
      <div
        v-for="stop in stopData"
        :key="stop.key"
        class="w-full max-w-2xl bg-white rounded-2xl shadow overflow-hidden"
      >
        <!-- Stop header -->
        <div class="px-5 pt-5 pb-3 flex items-center justify-between gap-2">
          <div class="flex items-center gap-2 min-w-0">
            <span class="text-base font-bold shrink-0">
              <span v-if="stop.status === 'fullfort'" class="text-green-600">✓</span>
              <span v-else-if="stop.status === 'pagar'" class="text-amber-500">⟳</span>
              <span v-else class="text-gray-400">🔒</span>
            </span>
            <p class="font-bold text-gray-900 text-base truncate">{{ stop.label }}</p>
          </div>
          <span
            class="text-xs px-3 py-1 rounded-full font-bold shrink-0"
            :class="{
              'bg-green-100 text-green-700': stop.status === 'fullfort',
              'bg-amber-100 text-amber-700': stop.status === 'pagar',
              'bg-gray-100 text-gray-500': stop.status === 'last',
            }"
          >
            {{ stop.status === 'fullfort' ? 'Fullført' : stop.status === 'pagar' ? 'Pågår' : 'Låst' }}
          </span>
        </div>

        <!-- Progress bar -->
        <div class="px-5 pb-3">
          <div class="flex items-center gap-2">
            <div class="flex-1 bg-gray-200 rounded-full h-2.5">
              <div
                class="h-2.5 rounded-full transition-all"
                :class="{
                  'bg-green-500': stop.status === 'fullfort',
                  'bg-amber-500': stop.status === 'pagar',
                  'bg-gray-300': stop.status === 'last',
                }"
                :style="{ width: `${stop.pct}%` }"
              />
            </div>
            <span class="text-xs font-bold text-gray-600 shrink-0">{{ stop.pct }}%</span>
          </div>
        </div>

        <!-- Task list -->
        <div class="px-5 pb-3 flex flex-col gap-1.5">
          <div
            v-for="{ task, score } in stop.tasks"
            :key="task.id"
            class="flex items-center justify-between text-sm px-3 py-2 rounded-lg"
            :class="score?.firstPassedAt ? 'bg-green-50' : 'bg-gray-50'"
          >
            <div class="flex items-center gap-2 min-w-0">
              <span
                class="shrink-0 font-bold"
                :class="score?.firstPassedAt ? 'text-green-600' : 'text-gray-300'"
              >
                {{ score?.firstPassedAt ? '✓' : '○' }}
              </span>
              <span class="text-gray-800 truncate">{{ task.title }}</span>
            </div>
            <div class="flex gap-3 text-xs text-gray-500 shrink-0 ml-2">
              <span v-if="score">
                <span class="font-semibold text-gray-700">{{ score.bestScore ?? 0 }}</span>p
              </span>
              <span v-if="score">{{ score.totalAttempts }} forsøk</span>
              <span v-if="score?.firstPassedAt" class="text-green-600">
                {{ formatDate(score.firstPassedAt) }}
              </span>
              <span v-if="!score" class="italic text-gray-400">ikke forsøkt</span>
            </div>
          </div>
        </div>

        <!-- Notebook toggle -->
        <div class="px-5 pb-5">
          <button
            class="btn-vn notebook-toggle-btn"
            @click="toggleNotebook(stop.key)"
          >
            {{ openNotebookStop === stop.key ? 'Skjul notater' : 'Se Notater' }}
          </button>
          <div v-if="openNotebookStop === stop.key" class="mt-3">
            <div
              v-if="stop.notebook?.studentReflection"
              class="bg-amber-50 border border-amber-200 rounded-lg px-4 py-3 text-sm text-gray-800 italic"
            >
              "{{ stop.notebook.studentReflection }}"
            </div>
            <p v-else class="text-xs text-gray-400 italic text-center py-2">
              Ingen refleksjon skrevet for dette stoppestedet.
            </p>
          </div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>
