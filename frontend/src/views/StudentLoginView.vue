<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import { useAuthStore } from '@/stores/AuthStore.ts'
import { useStudentStore } from '@/stores/StudentStore.ts'

const router = useRouter()
const authStore = useAuthStore()
const studentStore = useStudentStore()

const email = ref('student1@ntnu.no')
const password = ref('password123')
const serverError = ref('')
const isLoading = ref(false)
const fieldErrors = ref({ email: '', password: '' })

function validate(): boolean {
  fieldErrors.value = { email: '', password: '' }
  let valid = true

  if (!email.value.trim()) {
    fieldErrors.value.email = 'Epost kreves'
    valid = false
  }

  if (!password.value) {
    fieldErrors.value.password = 'Passord kreves'
    valid = false
  }

  return valid
}

async function handleLogin(): Promise<void> {
  serverError.value = ''
  if (!validate()) return

  isLoading.value = true
  try {
    await authStore.feideLogin({
      email: email.value,
      affiliation: 'student',
      password: password.value,
    })

    if (authStore.isTeacher) {
      authStore.clearSession()
      router.push({ name: 'teacher-login' })
      return
    }

    const headers = { Authorization: `Bearer ${authStore.token}` }

    const res = await fetch(`${import.meta.env.VITE_API_URL}/classrooms/me`, { headers })
    if (res.ok) {
      const body = await res.json()
      const classroomId = body.data?.id
      const institutionId = body.data?.institutionId
      if (classroomId && institutionId) {
        studentStore.setClassroom(classroomId, institutionId)
      }
      router.push({ name: 'student-home' })
      return
    }

    const profileRes = await fetch(`${import.meta.env.VITE_API_URL}/profiles/me`, { headers })
    if (profileRes.ok) {
      const profileBody = await profileRes.json()
      if (!profileBody.data?.avatarConfig) {
        router.push({ name: 'set-display-name' })
        return
      }
    }

    const statusRes = await fetch(`${import.meta.env.VITE_API_URL}/classrooms/me/status`, {
      headers,
    })
    if (statusRes.ok) {
      router.push({ name: 'awaiting-approval' })
    } else {
      router.push({ name: 'join-classroom' })
    }
  } catch (e) {
    const status = (e as Error & { status?: number }).status
    serverError.value = status === 401
      ? 'Feil brukernavn eller passord'
      : e instanceof Error ? e.message : 'Innlogging feilet. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <SceneComponent title="Feide elev innlogging" previous="/start" background="/whiteBgImage.jpg">
    <div class="login-shell">
      <div class="login-card-stage">
        <div class="login-card">
          <div class="login-toolbar">
            <span class="login-locale">Norsk</span>
          </div>

          <div class="login-content">
            <div class="login-header">
              <h2>Logg inn med Feide</h2>
              <p>Logg inn via Feide for å få tilgang som elev.</p>
            </div>

            <div class="login-divider" />

            <div class="affiliation-box">
              <span class="affiliation-badge" aria-hidden="true">i</span>
              <div>
                <p class="affiliation-label">Tilknytning</p>
                <p class="affiliation-value">Elev</p>
              </div>
            </div>

            <div class="login-divider" />

            <div v-if="serverError" class="error-banner" role="alert">
              {{ serverError }}
            </div>

            <div class="field">
              <label for="student-email">Brukernavn</label>
              <input
                id="student-email"
                v-model="email"
                type="email"
                autocomplete="email"
                :aria-invalid="!!fieldErrors.email"
                @keyup.enter="handleLogin"
              />
              <span v-if="fieldErrors.email" class="field-error" role="alert">
                {{ fieldErrors.email }}
              </span>
            </div>

            <div class="field">
              <label for="student-password">Passord</label>
              <input
                id="student-password"
                v-model="password"
                type="password"
                autocomplete="current-password"
                :aria-invalid="!!fieldErrors.password"
                @keyup.enter="handleLogin"
              />
              <span v-if="fieldErrors.password" class="field-error" role="alert">
                {{ fieldErrors.password }}
              </span>
            </div>

            <button class="login-button btn-bare" :disabled="isLoading" @click="handleLogin">
              {{ isLoading ? 'Logger inn...' : 'Logg inn' }}
            </button>

            <button type="button" class="secondary-link btn-bare">
              Glemt brukernavn eller passord?
            </button>

            <div class="support-row">
              <span>Trenger du hjelp?</span>
              <span class="support-plus">+</span>
            </div>

            <button type="button" class="privacy-link btn-bare">
              Personvern og informasjonskapsler
            </button>
          </div>

          <div class="login-footer">Feide</div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
:deep(.scene) {
  height: 100dvh;
  min-height: 100dvh;
  overflow: hidden;
  background: #d8e0ec;
}

:deep(.scene-background) {
  display: none;
}

:deep(.scene-overlay) {
  opacity: 0;
}

:deep(.scene-vignette) {
  display: none;
}

:deep(.scene-content) {
  height: 100dvh;
  min-height: 0;
  background: #d8e0ec;
}

:deep(.scene-body) {
  min-height: 0;
  overflow: auto;
}

:deep(.nav-title) {
  color: #1f2937;
  -webkit-text-stroke-width: 0;
  -webkit-text-stroke-color: transparent;
  text-shadow: none;
  font-size: 1.5rem;
}

.login-shell {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 0.5rem 1rem 0.75rem;
}

.login-card-stage {
  width: 100%;
  display: flex;
  justify-content: center;
}

.login-card {
  width: 100%;
  max-width: 30rem;
  background: #ffffff;
  border: 1px solid #d7deea;
  box-shadow: 0 8px 24px rgba(56, 79, 118, 0.12);
  color: #1f2937;
}

.login-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  min-height: 2rem;
  padding: 0.5rem 0.75rem;
  background: #f7f9fc;
  border-bottom: 1px solid #dde4ef;
}

.login-locale {
  font-size: 0.85rem;
  color: #35538d;
}

.login-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem 1.6rem 1rem;
}

.login-header h2 {
  margin: 0 0 0.4rem;
  font-size: 1.7rem;
  font-weight: 700;
  color: #1f2937;
  -webkit-text-stroke-width: 0;
  -webkit-text-stroke-color: transparent;
  text-shadow: none;
}

.login-header p {
  margin: 0;
  font-size: 1rem;
  line-height: 1.4;
  color: #374151;
}

.login-divider {
  border-top: 1px solid #d9e1ee;
}

.affiliation-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.affiliation-badge {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 999px;
  border: 2px solid #d12b2b;
  color: #d12b2b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  font-weight: 700;
  flex-shrink: 0;
}

.affiliation-label {
  margin: 0 0 0.15rem;
  font-size: 0.85rem;
  color: #4b5563;
}

.affiliation-value {
  margin: 0;
  font-size: 0.9rem;
  color: #111827;
  font-weight: 500;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

label {
  color: #111827;
  font-weight: 400;
  font-size: 1.05rem;
}

input:not([type="checkbox"]):not([type="radio"]):not([type="range"]) {
  height: 2.95rem;
  padding: 0.65rem 0.8rem;
  border: 1px solid #5f79be;
  border-radius: 0;
  background: #fff;
  color: #111827;
  font-size: 1.05rem;
  font-weight: 400;
}

input:focus {
  outline: 2px solid rgba(95, 121, 190, 0.22);
  outline-offset: 1px;
}

.field-error {
  color: #b91c1c;
  font-size: 0.8rem;
  line-height: 1.3;
}

.error-banner {
  background: #fff1f2;
  border: 1px solid #f1b5bb;
  color: #991b1b;
  font-weight: 600;
  padding: 0.6rem 0.75rem;
  font-size: 0.85rem;
  line-height: 1.35;
}

.login-button {
  height: 2.95rem;
  border: none;
  border-radius: 0;
  background: #8eb2ea;
  color: #1f2937;
  font-size: 1.05rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  width: 100%;
}

.login-button:hover:not(:disabled) {
  background: #81a8e5;
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.secondary-link,
.privacy-link {
  padding: 0;
  border: none;
  background: transparent;
  color: #2f4a84;
  font-size: 0.9rem;
  text-align: left;
  text-decoration: underline;
  cursor: pointer;
}

.support-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 0;
  border-top: 1px solid #d9e1ee;
  border-bottom: 1px solid #d9e1ee;
  font-size: 1rem;
  color: #1f2937;
}

.support-plus {
  font-size: 1.25rem;
  line-height: 1;
  color: #35538d;
}

.login-footer {
  padding: 0.75rem 1rem;
  background: #dbe7f7;
  text-align: center;
  font-size: 0.8rem;
  color: #4b5563;
}

@media (max-width: 480px) {
  .login-shell {
    padding: 1rem 0.5rem;
  }

  .login-card {
    max-width: 100%;
  }
}
</style>
