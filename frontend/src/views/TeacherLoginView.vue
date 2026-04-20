<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from "vue-router";
import SceneComponent from '../components/SceneComponent.vue';
import { useAuthStore } from "@/stores/AuthStore.ts";

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const serverError = ref('')
const isLoading = ref(false)

const fieldErrors = ref ({ email: '', password: '' })

function validate(): boolean {
  fieldErrors.value = { email: '', password: '' }
  let valid = true

  if (!email.value.trim()) {
    fieldErrors.value.email = 'Epost kreves'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    fieldErrors.value.email = 'Ugyldig epostformat'
    valid = false
  }

  if (!password.value) {
    fieldErrors.value.password = 'Passord kreves'
    valid = false
  } else if (password.value.length < 8) {
    fieldErrors.value.password = 'Passordet må være minst 8 tegn'
    valid = false
  }

  return valid
}

async function handleLogin(): Promise<void> {
  serverError.value = ''
  if (!validate()) return

  isLoading.value = true
  try {
    await authStore.feideLogin({ email: email.value, affiliation: "instructor", password: password.value})
    router.push({ name: 'teacher-home' })
  } catch (e) {
    serverError.value = e instanceof Error ? e.message : 'Innlogging feilet. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <SceneComponent
    title="Feide Lærer innlogging"
    previous="/start"
    background="/StartBackground.jpg"
  >
    <div class="login-container">
      <div class="login-card">
        <div v-if="serverError" class="error-banner" role="alert">
          {{ serverError }}
        </div>

        <div class="field">
          <label for="teacher-email">Epost</label>
          <input
            id="teacher-email"
            type="email"
            autocomplete="email"
            v-model="email"
            :aria-invalid="!!fieldErrors.email"
            @keyup.enter="handleLogin"
          />
          <span v-if="fieldErrors.email" class="field-error" role="alert">
            {{ fieldErrors.email }}
          </span>
        </div>

        <div class="field">
          <label for="teacher-password">Passord</label>
          <input
            id="teacher-password"
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

        <button :disabled="isLoading" @click="handleLogin">
          {{ isLoading ? 'Logger inn...' : 'Logg inn' }}
        </button>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.login-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.login-card {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
  max-width: 480px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

label {
  color: white;
  font-weight: 700;
  font-size: 1.25rem;
  -webkit-text-stroke: 2px #000000;
  paint-order: stroke fill;
}

.field-error {
  color: #fca5a5;
  font-size: 0.875rem;
  font-weight: 600;
  background: rgba(0, 0, 0, 0.65);
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
}

.error-banner {
  background: rgba(239, 68, 68, 0.9);
  color: white;
  font-weight: 600;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  text-align: center;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
