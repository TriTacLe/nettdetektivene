<script setup lang="ts">
import { ref, nextTick, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import { useAuthStore } from '@/stores/AuthStore.ts'
import { useStudentStore } from '@/stores/StudentStore.ts'

const router = useRouter()
const authStore = useAuthStore()
const studentStore = useStudentStore()

const email = ref('student1@test.no')
const password = ref('password123')
const serverError = ref('')
const isLoading = ref(false)
const loginShell = ref<HTMLElement | null>(null)
const loginCard = ref<HTMLElement | null>(null)
const loginScale = ref(1)

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
    await authStore.feideLogin({ email: email.value, affiliation: 'student', password: password.value })

    const emailPrefix = authStore.user?.email?.split('@')[0]
    if (authStore.user?.displayName === emailPrefix) {
      router.push({ name: 'set-display-name' })
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
    } else {
      const statusRes = await fetch(`${import.meta.env.VITE_API_URL}/classrooms/me/status`, { headers })
      if (statusRes.ok) {
        router.push({ name: 'awaiting-approval' })
      } else {
        router.push({ name: 'join-classroom' })
      }
    }
  } catch (e) {
    serverError.value = e instanceof Error ? e.message : 'Innlogging feilet. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
}

async function updateLoginScale() {
  await nextTick()

  const shell = loginShell.value
  const card = loginCard.value
  if (!shell || !card) return

  const previousTransform = card.style.transform
  card.style.transform = 'scale(1)'

  const shellWidth = shell.clientWidth
  const shellHeight = shell.clientHeight
  const cardWidth = card.offsetWidth
  const cardHeight = card.offsetHeight

  card.style.transform = previousTransform

  if (!shellWidth || !shellHeight || !cardWidth || !cardHeight) {
    loginScale.value = 1
    return
  }

  const widthScale = shellWidth / cardWidth
  const heightScale = shellHeight / cardHeight
  loginScale.value = Math.min(1, widthScale, heightScale)
}

onMounted(() => {
  updateLoginScale()
  window.addEventListener('resize', updateLoginScale)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateLoginScale)
})
</script>

<template>
  <SceneComponent
    title="Feide elev innlogging"
    previous="/start"
    next="/join-classroom"
    background="/whiteBgImage.jpg"
  >
    <div ref="loginShell" class="login-shell">
      <div class="login-card-stage">
        <div
          ref="loginCard"
          class="login-card"
          :style="{ transform: `scale(${loginScale})` }"
        >
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

            <button class="login-button" :disabled="isLoading" @click="handleLogin">
              {{ isLoading ? 'Logger inn...' : 'Logg inn' }}
            </button>

            <button type="button" class="secondary-link">
              Glemt brukernavn eller passord?
            </button>

            <div class="support-row">
              <span>Trenger du hjelp?</span>
              <span class="support-plus">+</span>
            </div>

            <button type="button" class="privacy-link">
              Personvern og informasjonskapsler
            </button>
          </div>

          <div class="login-footer">
            Feide
          </div>
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
}

:deep(.scene-background) {
  background: #ffffff;
  filter: brightness(1.35) saturate(0);
}

:deep(.scene-overlay) {
  opacity: 0;
}

:deep(.scene-content) {
  height: 100dvh;
  min-height: 0;
}

:deep(.scene-body) {
  min-height: 0;
  overflow: hidden;
}

:deep(.nav-title) {
  color: #1f2937;
  -webkit-text-stroke-width: 0;
  -webkit-text-stroke-color: transparent;
  text-shadow: none;
  font-size: clamp(1.1rem, 2.2vw, 2rem);
  margin: 0 0.75rem 0 1.25rem;
}

:deep(.navigation-header) {
  padding: clamp(0.65rem, 1.4vw, 1rem) clamp(0.85rem, 1.8vw, 1.5rem);
}

.login-shell {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(0.5rem, 1.2vw, 1rem);
  overflow: hidden;
}

.login-card-stage {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-card {
  width: 100%;
  max-width: clamp(16rem, 32vw, 17.25rem);
  background: #ffffff;
  border: 1px solid #d7deea;
  box-shadow: 0 8px 24px rgba(56, 79, 118, 0.12);
  color: #1f2937;
  transform-origin: center center;
  will-change: transform;
}

.login-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  min-height: clamp(1.5rem, 3vw, 1.75rem);
  padding: clamp(0.25rem, 0.8vw, 0.35rem) clamp(0.4rem, 1vw, 0.5rem);
  background: #f7f9fc;
  border-bottom: 1px solid #dde4ef;
}

.login-locale {
  font-size: clamp(0.65rem, 1.2vw, 0.7rem);
  color: #35538d;
}

.login-content {
  display: flex;
  flex-direction: column;
  gap: clamp(0.65rem, 1.4vw, 0.85rem);
  padding: clamp(0.7rem, 1.8vw, 0.85rem) clamp(0.55rem, 1.4vw, 0.6rem) clamp(0.6rem, 1.4vw, 0.7rem);
}

.login-header h2 {
  margin: 0 0 clamp(0.35rem, 1vw, 0.55rem);
  font-size: clamp(0.95rem, 1.8vw, 1rem);
  font-weight: 700;
  color: #1f2937;
  -webkit-text-stroke-width: 0;
  -webkit-text-stroke-color: transparent;
  text-shadow: none;
}

.login-header p {
  margin: 0;
  font-size: clamp(0.68rem, 1.2vw, 0.7rem);
  line-height: 1.35;
  color: #374151;
}

.login-divider {
  border-top: 1px solid #d9e1ee;
}

.affiliation-box {
  display: flex;
  align-items: center;
  gap: clamp(0.45rem, 1vw, 0.55rem);
}

.affiliation-badge {
  width: clamp(1.2rem, 2.5vw, 1.4rem);
  height: clamp(1.2rem, 2.5vw, 1.4rem);
  border-radius: 999px;
  border: 2px solid #d12b2b;
  color: #d12b2b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: clamp(0.85rem, 1.8vw, 0.95rem);
  font-weight: 700;
}

.affiliation-label {
  margin: 0 0 0.1rem;
  font-size: clamp(0.62rem, 1.1vw, 0.65rem);
  color: #4b5563;
}

.affiliation-value {
  margin: 0;
  font-size: clamp(0.68rem, 1.2vw, 0.7rem);
  color: #111827;
  font-weight: 500;
}

.field {
  display: flex;
  flex-direction: column;
  gap: clamp(0.22rem, 0.8vw, 0.3rem);
}

label {
  color: #111827;
  font-weight: 400;
  font-size: clamp(0.68rem, 1.2vw, 0.72rem);
}

input {
  height: clamp(2rem, 4.2vw, 2.15rem);
  padding: clamp(0.4rem, 0.9vw, 0.45rem) clamp(0.45rem, 1vw, 0.55rem);
  border: 1px solid #5f79be;
  border-radius: 0;
  background: #fff;
  color: #111827;
  font-size: clamp(0.76rem, 1.3vw, 0.8rem);
}

input:focus {
  outline: 2px solid rgba(95, 121, 190, 0.22);
  outline-offset: 1px;
}

.field-error {
  color: #b91c1c;
  font-size: clamp(0.64rem, 1.1vw, 0.68rem);
  line-height: 1.3;
}

.error-banner {
  background: #fff1f2;
  border: 1px solid #f1b5bb;
  color: #991b1b;
  font-weight: 600;
  padding: clamp(0.45rem, 1vw, 0.55rem) clamp(0.5rem, 1.1vw, 0.65rem);
  font-size: clamp(0.68rem, 1.2vw, 0.72rem);
  line-height: 1.35;
}

.login-button {
  height: clamp(2rem, 4.2vw, 2.15rem);
  border: none;
  border-radius: 0;
  background: #8eb2ea;
  color: #1f2937;
  font-size: clamp(0.74rem, 1.3vw, 0.78rem);
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.login-button:hover:not(:disabled) {
  background: #81a8e5;
}

.secondary-link,
.privacy-link {
  padding: 0;
  border: none;
  background: transparent;
  color: #2f4a84;
  font-size: clamp(0.62rem, 1.1vw, 0.64rem);
  text-align: left;
  text-decoration: underline;
  cursor: default;
}

.support-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: clamp(0.28rem, 0.8vw, 0.35rem) 0;
  border-top: 1px solid #d9e1ee;
  border-bottom: 1px solid #d9e1ee;
  font-size: clamp(0.68rem, 1.2vw, 0.7rem);
  color: #1f2937;
}

.support-plus {
  font-size: clamp(1rem, 2vw, 1.2rem);
  line-height: 1;
  color: #35538d;
}

.login-footer {
  padding: clamp(0.75rem, 1.6vw, 0.9rem) clamp(0.65rem, 1.4vw, 0.75rem) clamp(0.8rem, 1.8vw, 1rem);
  background: #dbe7f7;
  text-align: center;
  font-size: clamp(0.64rem, 1.1vw, 0.68rem);
  color: #4b5563;
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  :deep(.nav-title) {
    font-size: 1rem;
    margin: 0 0.5rem 0 0.75rem;
  }

  :deep(.navigation-header) {
    padding: 0.65rem 0.85rem;
  }

  .login-card {
    max-width: min(100%, 19rem);
  }
}

@media (max-width: 480px) {
  .login-shell {
    padding: 0.35rem;
  }

  .login-card {
    max-width: 100%;
  }
}
</style>
