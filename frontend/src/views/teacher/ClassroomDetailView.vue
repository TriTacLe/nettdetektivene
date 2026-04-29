<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { ClipboardList, Bell, Settings, Gamepad2, X, Search } from 'lucide-vue-next'
import {
  getClassroomOptions,
  getMyNotificationsOptions,
  getStudentsOptions,
  getStudentsQueryKey,
  getClassroomLeaderboardOptions,
  getClassroomTasksOptions,
  approveMutation,
  removeMutation,
} from '@/generated-client/@tanstack/vue-query.gen.ts'
import apiClient from '@/services/ApiService'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const classroomId = route.params.id as string

const { data } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroom = computed(() => data.value?.data ?? null)

const { data: studentsData, isLoading: studentsLoading } = useQuery(getStudentsOptions({ path: { classroomId } }))
const allStudents = computed(() => studentsData.value?.data ?? [])

const { data: leaderboardData } = useQuery(getClassroomLeaderboardOptions({ path: { classroomId } }))
const leaderboard = computed(() => leaderboardData.value?.data ?? [])

const { data: tasksData } = useQuery(getClassroomTasksOptions({ path: { classroomId } }))
const totalTasks = computed(() => tasksData.value?.data?.length ?? 0)

const { data: notificationsData } = useQuery({ ...getMyNotificationsOptions(), refetchInterval: 5000 })
const unreadCount = computed(() =>
  (notificationsData.value?.data ?? []).filter((n) => !n.isRead && n.classroomId === classroomId).length
)

// --- Leaderboard lookup by userId ---
function getLeaderboardEntry(userId?: string) {
  if (!userId) return null
  return leaderboard.value.find((e) => e.userId === userId) ?? null
}

// --- Student search ---
const searchQuery = ref('')
const showSearch = computed(() => allStudents.value.length >= 10)

const sortedStudents = computed(() => {
  return [...allStudents.value].sort((a, b) => {
    if (a.isAllowed !== b.isAllowed) return a.isAllowed ? -1 : 1
    const scoreA = getLeaderboardEntry(a.userId)?.totalScore ?? 0
    const scoreB = getLeaderboardEntry(b.userId)?.totalScore ?? 0
    return scoreB - scoreA
  })
})

const filteredStudents = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return sortedStudents.value
  return sortedStudents.value.filter((s) => s.displayName?.toLowerCase().includes(q))
})


const approve = useMutation({
  ...approveMutation(),
  onSuccess: () => queryClient.invalidateQueries({ queryKey: getStudentsQueryKey({ path: { classroomId } }) }),
})

const remove = useMutation({
  ...removeMutation(),
  onSuccess: () => {
    confirmTarget.value = null
    queryClient.invalidateQueries({ queryKey: getStudentsQueryKey({ path: { classroomId } }) })
  },
})

function approveStudent(userId: string, event: Event) {
  event.stopPropagation()
  approve.mutate({ path: { classroomId, userId } })
}

const resetName = useMutation({
  mutationFn: async ({ userId }: { userId: string }) => {
    const { data } = await apiClient.post(`/classrooms/${classroomId}/students/${userId}/reset-name`)
    return data
  },
  onSuccess: () => {
    resetTarget.value = null
    queryClient.invalidateQueries({ queryKey: getStudentsQueryKey({ path: { classroomId } }) })
  },
})

const resetTarget = ref<{ userId: string; displayName: string } | null>(null)
const resetTriggerRef = ref<HTMLButtonElement | null>(null)

function requestReset(userId: string, displayName: string, event: Event) {
  event.stopPropagation()
  resetTriggerRef.value = event.currentTarget as HTMLButtonElement
  resetTarget.value = { userId, displayName }
  nextTick(() => {
    const dialog = document.getElementById('reset-dialog')
    const cancelBtn = dialog?.querySelector<HTMLButtonElement>('[data-action="cancel"]')
    cancelBtn?.focus()
  })
}

function confirmReset() {
  if (!resetTarget.value) return
  resetName.mutate({ userId: resetTarget.value.userId })
}

function cancelReset() {
  resetTarget.value = null
  nextTick(() => resetTriggerRef.value?.focus())
}

function onResetDialogKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') cancelReset()
  if (e.key === 'Tab') {
    const dialog = document.getElementById('reset-dialog')
    const focusable = dialog?.querySelectorAll<HTMLElement>('button:not([disabled])')
    if (!focusable || focusable.length === 0) return
    const first = focusable[0] as HTMLElement | undefined
    const last = focusable[focusable.length - 1] as HTMLElement | undefined
    if (!first || !last) return
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault()
      last.focus()
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault()
      first.focus()
    }
  }
}

const confirmTarget = ref<{ userId: string; displayName: string } | null>(null)
const confirmTriggerRef = ref<HTMLButtonElement | null>(null)

function requestRemove(userId: string, displayName: string, event: Event) {
  event.stopPropagation()
  confirmTriggerRef.value = event.currentTarget as HTMLButtonElement
  confirmTarget.value = { userId, displayName }
  nextTick(() => {
    const dialog = document.getElementById('remove-dialog')
    const cancelBtn = dialog?.querySelector<HTMLButtonElement>('[data-action="cancel"]')
    cancelBtn?.focus()
  })
}

function confirmRemove() {
  if (!confirmTarget.value) return
  remove.mutate({ path: { classroomId, userId: confirmTarget.value.userId } })
}

function cancelRemove() {
  confirmTarget.value = null
  nextTick(() => confirmTriggerRef.value?.focus())
}

function onDialogKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') cancelRemove()
  if (e.key === 'Tab') {
    const dialog = document.getElementById('remove-dialog')
    const focusable = dialog?.querySelectorAll<HTMLElement>('button:not([disabled])')
    if (!focusable || focusable.length === 0) return
    const first = focusable[0] as HTMLElement | undefined
    const last = focusable[focusable.length - 1] as HTMLElement | undefined
    if (!first || !last) return
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault()
      last.focus()
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault()
      first.focus()
    }
  }
}

// --- Helpers ---
function studentAriaLabel(s: any): string {
  if (!s.isAllowed) return `${s.displayName}, venter pa godkjenning`
  const entry = getLeaderboardEntry(s.userId)
  const score = entry ? `, poeng ${entry.totalScore}` : ''
  const rank = entry?.rank ? `, rangert ${entry.rank}` : ''
  return `Se progresjon for ${s.displayName}${score}${rank}`
}

function navigateToStudent(student: any) {
  if (!student.isAllowed) return
  router.push({ name: 'classroom-student-progress', params: { id: classroomId, userId: student.userId } })
}

const menuItems = [
  { title: 'Oversikt Oppgaver', subtitle: 'Oppgaver i spillet', icon: ClipboardList, route: 'classroom-tasks' },
  { title: 'Varsler', subtitle: 'Meldinger fra klassen', icon: Bell, route: 'notifications', showBadge: true },
  { title: 'Klasseromsstyring', subtitle: 'Rediger innstillinger', icon: Settings, route: 'classroom-settings' },
  { title: 'Test Spillet', subtitle: 'Spill som elev', icon: Gamepad2, route: 'classroom-test-game' },
]

function handleMenuItem(item: (typeof menuItems)[0]) {
  if (item.route === 'notifications') {
    router.push({ name: 'teacher-notifications', query: { classroomId } })
    return
  }
  router.push({ name: item.route, params: { id: classroomId } })
}
</script>

<template>
  <SceneComponent
    :title="classroom?.title || 'Klasserom'"
    previous="/teacher-home"
    background="/StartBackground.jpg"
    dim-overlay
  >
    <div class="detail-wrapper">
      <div class="detail-layout">

        <section class="panel-students" aria-label="Elever">

          <div class="divider-heading">
            <div class="divider-line" />
            <h2 class="section-label">Elever ({{ allStudents.length }})</h2>
            <div class="divider-line" />
          </div>

          <div v-if="showSearch" class="search-row">
            <Search :size="14" class="search-icon" aria-hidden="true" />
            <input
              v-model="searchQuery"
              type="text"
              class="search-input"
              placeholder="Sok elev..."
              aria-label="Sok etter elev"
            />
          </div>

          <div v-if="studentsLoading" class="section-empty">Laster elever...</div>

          <div v-else-if="allStudents.length === 0" class="empty-state">
            <p class="empty-title">Ingen elever enna</p>
            <p class="empty-hint">
              Del klasseromskoden
              <span class="empty-code">{{ classroom?.joinCode ?? '---' }}</span>
              sa elevene kan bli med.
            </p>
          </div>

          <ul
            v-else
            class="student-list"
            role="list"
            aria-label="Elever i klasserommet"
          >
            <li
              v-for="(student, i) in filteredStudents"
              :key="student.userId"
              class="student-row-wrapper"
              :style="{ animationDelay: `${0.04 + i * 0.02}s` }"
            >
              <div v-if="!student.isAllowed" class="student-row student-row--pending">
                <span class="sr-name">{{ student.displayName }}</span>
                <span class="status-badge status-badge--pending">
                  <span class="status-dot status-dot--amber" aria-hidden="true" />
                  Venter
                </span>
                <div class="sr-actions">
                  <button
                    class="sr-btn sr-btn--approve"
                    :aria-label="`Godkjenn ${student.displayName}`"
                    @click="student.userId && approveStudent(student.userId, $event)"
                  >Godkjenn</button>
                  <button
                    class="sr-btn sr-btn--icon"
                    :aria-label="`Fjern ${student.displayName} fra klasserommet`"
                    @click="student.userId && requestRemove(student.userId, student.displayName ?? 'Elev', $event)"
                  ><X :size="12" aria-hidden="true" /></button>
                </div>
              </div>

              <button
                v-else
                class="student-row student-row--approved"
                :aria-label="studentAriaLabel(student)"
                @click="navigateToStudent(student)"
              >
                <span class="sr-name">{{ student.displayName }}</span>

                <span class="sr-stat">{{ getLeaderboardEntry(student.userId)?.totalScore ?? 0 }} p</span>
                <span class="sr-stat">Lv. {{ getLeaderboardEntry(student.userId)?.xpLevel ?? 1 }}</span>
                <span class="sr-stat">#{{ getLeaderboardEntry(student.userId)?.rank ?? '-' }}</span>

                <div v-if="totalTasks > 0" class="sr-progress">
                  <div class="sr-progress-bar">
                    <div
                      class="sr-progress-fill"
                      :style="{ width: `${Math.min(100, ((getLeaderboardEntry(student.userId)?.totalScore ?? 0) / (totalTasks * 100)) * 100)}%` }"
                    ></div>
                  </div>
                </div>

                <div class="sr-actions">
                  <button
                    class="sr-btn sr-btn--reset"
                    :aria-label="`Fjern brukernavn for ${student.displayName}`"
                    @click="student.userId && requestReset(student.userId, student.displayName ?? 'Elev', $event)"
                  >Fjern navn</button>
                  <button
                    class="sr-btn sr-btn--icon"
                    :aria-label="`Fjern ${student.displayName} fra klasserommet`"
                    @click="student.userId && requestRemove(student.userId, student.displayName ?? 'Elev', $event)"
                  ><X :size="12" aria-hidden="true" /></button>
                </div>

                <span class="sr-chevron" aria-hidden="true">&rsaquo;</span>
              </button>
            </li>

            <li v-if="showSearch && filteredStudents.length === 0" class="section-empty">
              Ingen treff for &laquo;{{ searchQuery }}&raquo;
            </li>
          </ul>
        </section>

        <aside class="panel-sidebar">

          <div class="code-card">
            <span class="code-label">Klasseromskode</span>
            <span class="code-value">{{ classroom?.joinCode ?? '---' }}</span>
          </div>

          <div class="divider-heading">
            <div class="divider-line" />
            <h2 class="section-label section-label-sm">Mer</h2>
            <div class="divider-line" />
          </div>

          <div class="menu-column">
            <button
              v-for="item in menuItems"
              :key="item.title"
              class="card-menu"
              :aria-label="`${item.title} — ${item.subtitle}`"
              @click="handleMenuItem(item)"
            >
              <span
                v-if="item.showBadge && unreadCount > 0"
                class="badge"
                :aria-label="`${unreadCount} uleste varsler`"
              >{{ unreadCount }}</span>
              <component :is="item.icon" :size="18" aria-hidden="true" class="menu-icon" />
              <span class="menu-title">{{ item.title }}</span>
              <span class="menu-subtitle">{{ item.subtitle }}</span>
              <span class="chevron-menu" aria-hidden="true">&rsaquo;</span>
            </button>
          </div>
        </aside>

      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="confirmTarget"
        class="dialog-backdrop"
        @click.self="cancelRemove"
        @keydown="onDialogKeydown"
      >
        <div
          id="remove-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="remove-dialog-title"
          class="dialog-box"
        >
          <h3 id="remove-dialog-title" class="dialog-title">
            Fjern {{ confirmTarget.displayName }}?
          </h3>
          <p class="dialog-body">
            Eleven vil miste tilgang til klasserommet.
          </p>
          <div class="dialog-actions">
            <button
              data-action="cancel"
              class="dialog-btn dialog-btn--cancel"
              @click="cancelRemove"
            >Avbryt</button>
            <button
              class="dialog-btn dialog-btn--danger"
              :disabled="remove.isPending.value"
              @click="confirmRemove"
            >{{ remove.isPending.value ? 'Fjerner...' : 'Fjern' }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="resetTarget"
        class="dialog-backdrop"
        @click.self="cancelReset"
        @keydown="onResetDialogKeydown"
      >
        <div
          id="reset-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="reset-dialog-title"
          class="dialog-box"
        >
          <h3 id="reset-dialog-title" class="dialog-title">
            Fjern brukernavn for {{ resetTarget.displayName }}?
          </h3>
          <p class="dialog-body">
            Brukernavnet blir tilbakestilt til e-postprefikset.
          </p>
          <div class="dialog-actions">
            <button
              data-action="cancel"
              class="dialog-btn dialog-btn--cancel"
              @click="cancelReset"
            >Avbryt</button>
            <button
              class="dialog-btn dialog-btn--danger"
              :disabled="resetName.isPending.value"
              @click="confirmReset"
            >{{ resetName.isPending.value ? 'Tilbakestiller...' : 'Fjern Brukernavn' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </SceneComponent>
</template>

<style scoped>
.detail-wrapper {
  flex: 1;
  display: flex;
  justify-content: flex-start;
  width: 100%;
  padding: 1rem 3rem 2rem;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  width: 100%;
  max-width: 80rem;
  align-items: start;
}

@media (max-width: 768px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .panel-sidebar {
    order: -1;
  }
}

.panel-students {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.panel-sidebar {
  display: flex;
  flex-direction: column;
  gap: 0;
  position: sticky;
  top: 0;
}

.code-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
  padding: 1rem;
  margin-bottom: 2rem;
  border-radius: 0.75rem;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  animation: slideUp 0.5s ease-out both;
}

.code-label {
  font-size: 0.6875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.55);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.code-value {
  font-size: 1.375rem;
  font-weight: 800;
  color: white;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  letter-spacing: 0.12em;
}

.divider-heading {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  animation: slideUp 0.5s ease-out both;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: rgba(255, 255, 255, 0.2);
}

.section-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.875rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  margin: 0;
  white-space: nowrap;
}

.section-label-sm {
  font-size: 0.8125rem;
}

.search-row {
  position: relative;
  margin-bottom: 0.625rem;
  animation: slideUp 0.5s ease-out both;
}

.search-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 0.625rem 0.875rem 0.625rem 2.25rem;
  border: 1.5px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.625rem;
  background: rgba(255, 255, 255, 0.08);
  color: white;
  font-size: 0.8125rem;
  outline: none;
  transition: border-color 0.15s, background 0.15s;
  box-sizing: border-box;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.search-input:focus {
  border-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

.student-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.student-row-wrapper {
  animation: slideUp 0.4s ease-out both;
}

.student-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: none;
  border-radius: 0.625rem;
  background: rgba(255, 255, 255, 0.95);
  text-align: left;
  font-family: inherit;
  font-size: inherit;
  position: relative;
}

.student-row--approved {
  cursor: pointer;
  transition: background 0.15s;
}

.student-row--approved:hover {
  background: rgba(255, 255, 255, 1);
}

.student-row--approved:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

.student-row--pending {
  background: rgba(255, 255, 255, 0.88);
}

.sr-name {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 5rem;
  flex-shrink: 1;
}

.sr-stat {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #6b7280;
  white-space: nowrap;
  flex-shrink: 0;
}

.sr-progress {
  flex: 1;
  min-width: 3rem;
  max-width: 6rem;
}

.sr-progress-bar {
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.sr-progress-fill {
  height: 100%;
  background: #2563eb;
  border-radius: 2px;
  transition: width 0.4s ease;
}

.sr-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-shrink: 0;
  margin-left: auto;
}

.sr-btn {
  border: none;
  border-radius: 0.375rem;
  font-size: 0.6875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  white-space: nowrap;
}

.sr-btn:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 1px;
}

.sr-btn--approve {
  padding: 0.25rem 0.625rem;
  background: #15803d;
  color: white;
}

.sr-btn--approve:hover {
  background: #166534;
}

.sr-btn--reset {
  padding: 0.25rem 0.625rem;
  background: #fee2e2;
  color: #b91c1c;
  border: 1px solid #fca5a5;
  border-radius: 0.375rem;
}

.sr-btn--reset:hover {
  background: #fecaca;
  color: #991b1b;
  border-color: #f87171;
}

.sr-btn--icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  padding: 0;
  background: transparent;
  color: #d1d5db;
}

.sr-btn--icon:hover {
  color: #dc2626;
  background: #fef2f2;
}

.sr-chevron {
  color: #d1d5db;
  font-size: 0.875rem;
  font-weight: 700;
  flex-shrink: 0;
  transition: transform 0.15s, color 0.15s;
}

.student-row--approved:hover .sr-chevron {
  transform: translateX(2px);
  color: #9ca3af;
}

/* ---- Status badge ---- */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.625rem;
  font-weight: 600;
  padding: 0.125rem 0.4rem;
  border-radius: 9999px;
  white-space: nowrap;
}

.status-badge--pending {
  background: #fef3c7;
  color: #92400e;
}

.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot--amber {
  background: #d97706;
}

.empty-state {
  text-align: center;
  padding: 2.5rem 1rem;
  animation: slideUp 0.5s ease-out both;
}

.empty-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 0.5rem;
}

.empty-hint {
  font-size: 0.8125rem;
  color: rgba(255, 255, 255, 0.65);
  margin: 0;
  line-height: 1.5;
}

.empty-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 0.08em;
}

.section-empty {
  text-align: center;
  color: rgba(255, 255, 255, 0.75);
  font-size: 0.8125rem;
  margin: 0.75rem 0;
  animation: slideUp 0.5s ease-out both;
}

.menu-column {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.card-menu {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.625rem;
  width: 100%;
  padding: 0.875rem 1.125rem;
  border: none;
  border-radius: 0.75rem;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(8px);
  box-shadow: 0 2px 8px rgb(0 0 0 / 0.1);
  text-align: left;
  cursor: pointer;
  animation: slideUp 0.5s ease-out both;
  transition: background 0.15s, transform 0.15s, box-shadow 0.15s;
  min-height: 3rem;
}

.card-menu:hover {
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 2px 8px rgb(0 0 0 / 0.12);
  transform: translateY(-1px);
}

.card-menu:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

.menu-icon {
  color: #57534e;
  flex-shrink: 0;
}

.menu-title {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #111827;
  flex: 1;
}

.menu-subtitle {
  display: none;
}

@media (max-width: 768px) {
  .menu-subtitle {
    display: inline;
    font-size: 0.75rem;
    color: #6b7280;
    margin-left: auto;
  }
}

.chevron-menu {
  color: #9ca3af;
  font-size: 1rem;
  font-weight: 700;
  flex-shrink: 0;
  transition: transform 0.15s;
}

.card-menu:hover .chevron-menu {
  transform: translateX(2px);
}

.badge {
  position: absolute;
  top: -0.375rem;
  right: -0.375rem;
  font-size: 0.6875rem;
  font-weight: 700;
  border-radius: 9999px;
  min-width: 1.25rem;
  height: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.3rem;
  background: #c05555;
  color: white;
  box-shadow: 0 1px 4px rgb(192 85 85 / 0.3);
}

.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  padding: 1rem;
}

.dialog-box {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  max-width: 22rem;
  width: 100%;
  box-shadow: 0 8px 32px rgb(0 0 0 / 0.25);
}

.dialog-title {
  font-size: 1rem;
  font-weight: 700;
  color: #111827;
  margin: 0 0 0.5rem;
}

.dialog-body {
  font-size: 0.875rem;
  color: #4b5563;
  margin: 0 0 1.25rem;
  line-height: 1.5;
}

.dialog-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.dialog-btn {
  padding: 0.5rem 1.25rem;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  min-height: 44px;
  min-width: 44px;
  transition: background 0.15s;
}

.dialog-btn:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

.dialog-btn--cancel {
  background: #f3f4f6;
  color: #374151;
}

.dialog-btn--cancel:hover {
  background: #e5e7eb;
}

.dialog-btn--danger {
  background: #dc2626;
  color: white;
}

.dialog-btn--danger:hover {
  background: #b91c1c;
}

.dialog-btn--danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
