<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ClassroomService from '@/services/ClassroomService.ts'
import SceneComponent from '@/components/SceneComponent.vue'

const route = useRoute()
const router = useRouter()

const classroomId = route.params.id as string
const error = ref<string | null>(null)

const instructorCount = ref<number | null>(null)

onMounted(async () => {
  try {
    const instructors = await ClassroomService.getInstructors(classroomId)
    instructorCount.value = instructors.length
  } catch {
    instructorCount.value = null
  }
})

const isLastInstructor = computed(() => instructorCount.value !== null && instructorCount.value <= 1)

async function deleteClassroom() {
  if (!confirm('Er du sikker på at du vil slette klasserommet? Dette kan ikke angres.')) return
  try {
    await ClassroomService.deleteClassroom(classroomId)
    router.push({ name: 'teacher-home' })
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke slette klasserommet'
  }
}

const showLeaveDialog = ref(false)
const leavePending = ref(false)
const leaveError = ref<string | null>(null)

function openLeaveDialog() {
  leaveError.value = null
  showLeaveDialog.value = true
  nextTick(() => {
    const cancelBtn = document.querySelector<HTMLButtonElement>('#leave-dialog [data-action="cancel"]')
    cancelBtn?.focus()
  })
}

function closeLeaveDialog() {
  showLeaveDialog.value = false
}

async function confirmLeave() {
  leaveError.value = null
  leavePending.value = true
  try {
    await ClassroomService.leaveClassroom(classroomId)
    router.push({ name: 'teacher-home' })
  } catch {
    leaveError.value = 'Kan ikke forlate klasserommet. Du er den eneste læreren.'
  } finally {
    leavePending.value = false
  }
}

function onLeaveDialogKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') closeLeaveDialog()
  if (e.key === 'Tab') {
    const dialog = document.getElementById('leave-dialog')
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

const showInviteDialog = ref(false)
const inviteEmail = ref('')
const inviteError = ref<string | null>(null)
const inviteSuccess = ref<string | null>(null)
const invitePending = ref(false)

function openInviteDialog() {
  inviteEmail.value = ''
  inviteError.value = null
  inviteSuccess.value = null
  showInviteDialog.value = true
  nextTick(() => {
    const input = document.getElementById('invite-email-input') as HTMLInputElement | null
    input?.focus()
  })
}

function closeInviteDialog() {
  showInviteDialog.value = false
}

async function submitInvite() {
  inviteError.value = null
  inviteSuccess.value = null
  const email = inviteEmail.value.trim()
  if (!email) {
    inviteError.value = 'E-postadresse er påkrevd'
    return
  }
  invitePending.value = true
  try {
    await ClassroomService.addInstructor(classroomId, email)
    inviteSuccess.value = `${email} ble lagt til som instruktor`
    inviteEmail.value = ''
    if (instructorCount.value !== null) {
      instructorCount.value++
    }
  } catch (e: unknown) {
    inviteError.value = e instanceof Error ? e.message : 'Kunne ikke legge til instruktor'
  } finally {
    invitePending.value = false
  }
}

function onInviteDialogKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') closeInviteDialog()
  if (e.key === 'Tab') {
    const dialog = document.getElementById('invite-dialog')
    const focusable = dialog?.querySelectorAll<HTMLElement>('input:not([disabled]), button:not([disabled])')
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

const menuItems = computed(() => [
  {
    title: 'Rediger Klasserom',
    subtitle: 'Endre navn og innstillinger',
    action: () => router.push({ name: 'edit-classroom', params: { id: classroomId } }),
  },
  {
    title: 'Slett Klasserom',
    subtitle: 'Slett klasserommet permanent',
    action: deleteClassroom,
    danger: true,
  },
  {
    title: 'Inviter Lærer',
    subtitle: 'Legg til en medinstruktør',
    action: openInviteDialog,
  },
  {
    title: 'Forlat Klasserom',
    subtitle: isLastInstructor.value
      ? 'Kan ikke forlate — du er eneste instruktør'
      : 'Fjern deg selv som instruktør',
    action: openLeaveDialog,
    danger: true,
    disabled: isLastInstructor.value,
  },
])
</script>

<template>
  <SceneComponent
    title="Klasseromsstyring"
    :previous="`/classroom/${classroomId}`"
    background="/StartBackground.jpg"
    dimOverlay
  >
    <div class="flex flex-col items-center justify-center flex-1 w-full px-6 pb-10 pt-4">
      <div class="menu-stack">
        <div
          v-if="error"
          class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded text-sm"
        >
          {{ error }}
        </div>

        <button
          v-for="item in menuItems"
          :key="item.title"
          :disabled="item.disabled"
          class="btn-vn menu-item"
          :class="{ 'btn-vn-danger': item.danger }"
          @click="item.action()"
        >
          <span class="menu-text">
            <span class="menu-title">{{ item.title }}</span>
            <span class="menu-subtitle">{{ item.subtitle }}</span>
          </span>
          <span v-if="!item.disabled" class="menu-arrow" aria-hidden="true">›</span>
        </button>
      </div>
    </div>

    <!-- Invite dialog -->
    <Teleport to="body">
      <div
        v-if="showInviteDialog"
        class="dialog-backdrop"
        @click.self="closeInviteDialog"
        @keydown="onInviteDialogKeydown"
      >
        <div
          id="invite-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="invite-dialog-title"
          class="dialog-box"
        >
          <h3 id="invite-dialog-title" class="dialog-title">
            Inviter lærer
          </h3>
          <p class="dialog-body">
            Skriv inn e-postadressen til læreren du vil legge til som medinstruktør.
          </p>

          <form @submit.prevent="submitInvite">
            <input
              id="invite-email-input"
              v-model="inviteEmail"
              type="email"
              class="dialog-input"
              placeholder="laerer@ntnu.no"
              :disabled="invitePending"
              aria-label="E-postadresse"
            />

            <div
              v-if="inviteError"
              class="dialog-feedback dialog-feedback--error"
            >
              {{ inviteError }}
            </div>
            <div
              v-if="inviteSuccess"
              class="dialog-feedback dialog-feedback--success"
            >
              {{ inviteSuccess }}
            </div>

            <div class="dialog-actions">
              <button
                type="button"
                class="btn-bare dialog-btn dialog-btn--cancel"
                @click="closeInviteDialog"
              >Lukk</button>
              <button
                type="submit"
                class="btn-bare dialog-btn dialog-btn--primary"
                :disabled="invitePending"
              >{{ invitePending ? 'Legger til...' : 'Legg til' }}</button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Leave classroom dialog -->
    <Teleport to="body">
      <div
        v-if="showLeaveDialog"
        class="dialog-backdrop"
        @click.self="closeLeaveDialog"
        @keydown="onLeaveDialogKeydown"
      >
        <div
          id="leave-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="leave-dialog-title"
          class="dialog-box"
        >
          <h3 id="leave-dialog-title" class="dialog-title">
            Forlat klasserommet?
          </h3>
          <p class="dialog-body">
            Du vil miste tilgang til dette klasserommet som instruktor.
          </p>

          <div
            v-if="leaveError"
            class="dialog-feedback dialog-feedback--error"
          >
            {{ leaveError }}
          </div>

          <div class="dialog-actions">
            <button
              data-action="cancel"
              class="btn-bare dialog-btn dialog-btn--cancel"
              @click="closeLeaveDialog"
            >Avbryt</button>
            <button
              class="btn-bare dialog-btn dialog-btn--danger"
              :disabled="leavePending"
              @click="confirmLeave"
            >{{ leavePending ? 'Forlater...' : 'Forlat' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </SceneComponent>
</template>

<style scoped>
.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  padding: 1rem;
}

.dialog-box {
  background: white;
  border-radius: 1rem;
  padding: 2rem;
  max-width: 24rem;
  width: 100%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
}

.dialog-title {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
  margin: 0 0 0.5rem;
  -webkit-text-stroke: 0;
  paint-order: normal;
  text-shadow: none;
}

.dialog-body {
  font-size: 0.875rem;
  color: #4b5563;
  margin: 0 0 1.25rem;
  line-height: 1.5;
}

.dialog-input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1.5px solid #d1d5db;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  color: #111827;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
  margin-bottom: 0.75rem;
}

.dialog-input:focus {
  border-color: #2563eb;
}

.dialog-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dialog-feedback {
  font-size: 0.8125rem;
  margin-bottom: 0.75rem;
  padding: 0.5rem 0.75rem;
  border-radius: 0.375rem;
}

.dialog-feedback--error {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.dialog-feedback--success {
  background: #f0fdf4;
  color: #15803d;
  border: 1px solid #bbf7d0;
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

.dialog-btn--primary {
  background: #2563eb;
  color: white;
}

.dialog-btn--primary:hover {
  background: #1d4ed8;
}

.dialog-btn--primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
</style>
