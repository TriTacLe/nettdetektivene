<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useFocusTrap } from '@/composables/UseFocusTrap'
import ClassroomService from '@/services/ClassroomService.ts'
import SceneComponent from '@/components/SceneComponent.vue'

const route = useRoute()
const router = useRouter()

const classroomId = route.params.id as string

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

const showDeleteDialog = ref(false)
const deletePending = ref(false)
const deleteError = ref<string | null>(null)

function openDeleteDialog() {
  deleteError.value = null
  showDeleteDialog.value = true
  nextTick(() => {
    const cancelBtn = document.querySelector<HTMLButtonElement>('#delete-dialog [data-action="cancel"]')
    cancelBtn?.focus()
  })
}

function closeDeleteDialog() {
  showDeleteDialog.value = false
}

async function confirmDelete() {
  deleteError.value = null
  deletePending.value = true
  try {
    await ClassroomService.deleteClassroom(classroomId)
    router.push({ name: 'teacher-home' })
  } catch (e: unknown) {
    deleteError.value = e instanceof Error ? e.message : 'Kunne ikke slette klasserommet'
  } finally {
    deletePending.value = false
  }
}

const { onKeydown: onDeleteDialogKeydown } = useFocusTrap('delete-dialog', closeDeleteDialog)

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
    nextTick(() => {
      const cancelBtn = document.querySelector<HTMLButtonElement>('#leave-dialog [data-action="cancel"]')
      cancelBtn?.focus()
    })
  }
}

const { onKeydown: onLeaveDialogKeydown } = useFocusTrap('leave-dialog', closeLeaveDialog)

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

const { onKeydown: onInviteDialogKeydown } = useFocusTrap('invite-dialog', closeInviteDialog)

const menuItems = computed(() => [
  {
    title: 'Rediger Klasserom',
    subtitle: 'Endre navn og innstillinger',
    action: () => router.push({ name: 'edit-classroom', params: { id: classroomId } }),
  },
  {
    title: 'Slett Klasserom',
    subtitle: 'Slett klasserommet permanent',
    action: openDeleteDialog,
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

          <form class="dialog-form" @submit.prevent="submitInvite">
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

    <!-- Delete classroom dialog -->
    <Teleport to="body">
      <div
        v-if="showDeleteDialog"
        class="dialog-backdrop"
        @click.self="closeDeleteDialog"
        @keydown="onDeleteDialogKeydown"
      >
        <div
          id="delete-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-dialog-title"
          class="dialog-box"
        >
          <h3 id="delete-dialog-title" class="dialog-title">
            Slett klasserommet?
          </h3>
          <p class="dialog-body">
            Er du sikker på at du vil slette klasserommet? Dette kan ikke angres.
          </p>

          <div
            v-if="deleteError"
            class="dialog-feedback dialog-feedback--error"
          >
            {{ deleteError }}
          </div>

          <div class="dialog-actions">
            <button
              data-action="cancel"
              class="btn-bare dialog-btn dialog-btn--cancel"
              @click="closeDeleteDialog"
            >Avbryt</button>
            <button
              class="btn-bare dialog-btn dialog-btn--danger"
              :disabled="deletePending"
              @click="confirmDelete"
            >{{ deletePending ? 'Sletter...' : 'Slett' }}</button>
          </div>
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

