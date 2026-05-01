<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useAudioStore } from '@/stores/AudioStore'
import { useAuthStore } from '@/stores/AuthStore'
import { Volume2, VolumeX, Zap, X, LogOut } from 'lucide-vue-next'

const emit = defineEmits<{ close: [] }>()

const audio = useAudioStore()
const auth = useAuthStore()

const musicVolume = ref(Math.round(audio.musicVolume * 100))
const sfxVolume = ref(Math.round(audio.sfxVolume * 100))
const textSpeed = ref(audio.textSpeed)

const closeBtn = ref<HTMLButtonElement | null>(null)
const modalRef = ref<HTMLElement | null>(null)
const previouslyFocused = ref<HTMLElement | null>(null)


onMounted(() => {
  previouslyFocused.value = document.activeElement as HTMLElement | null
  nextTick(() => closeBtn.value?.focus())
})

function close() {
  emit('close')
  nextTick(() => previouslyFocused.value?.focus())
}

function getFocusable(): HTMLElement[] {
  if (!modalRef.value) return []
  return Array.from(
    modalRef.value.querySelectorAll<HTMLElement>(
      'button:not(:disabled), input:not(:disabled)'
    )
  )
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    close()
    return
  }
  if (e.key !== 'Tab') return
  e.preventDefault()
  const elements = getFocusable()
  if (elements.length === 0) return
  const active = document.activeElement
  const currentIdx = elements.indexOf(active as HTMLElement)

  if (e.shiftKey) {
    const prev = currentIdx <= 0 ? elements.length - 1 : currentIdx - 1
    elements[prev].focus()
  } else {
    const next = currentIdx >= elements.length - 1 ? 0 : currentIdx + 1
    elements[next].focus()
  }
}

function onMusicInput() {
  audio.setMusicVolume(musicVolume.value / 100)
}

function onSfxInput() {
  audio.setSfxVolume(sfxVolume.value / 100)
}

function onTextSpeedInput() {
  audio.setTextSpeed(textSpeed.value)
}

function logout() {
  auth.logout()
}
</script>

<template>
  <div
    class="modal-backdrop"
    tabindex="-1"
    @click.self="close"
    @keydown="onKeydown"
  >
    <div ref="modalRef" class="modal" role="dialog" aria-modal="true" aria-label="Innstillinger">
      <div class="modal-header">
        <h2 class="modal-title no-stroke">Innstillinger</h2>
        <button
          ref="closeBtn"
          class="close-button btn-bare"
          aria-label="Lukk innstillinger"
          @click="close"
        >
          <X :size="24" />
        </button>
      </div>

      <div class="modal-body">
        <section class="setting-section">
          <div class="setting-row">
            <div class="setting-label">
              <Volume2 v-if="!audio.musicMuted" :size="22" class="setting-icon" />
              <VolumeX v-else :size="22" class="setting-icon setting-icon--muted" />
              <label for="music-volume">Musikk</label>
            </div>
            <button
              class="toggle-button btn-bare"
              :class="{ 'toggle-button--off': audio.musicMuted }"
              role="switch"
              :aria-checked="!audio.musicMuted"
              :aria-label="audio.musicMuted ? 'Skru på musikk' : 'Demp musikk'"
              @click="audio.toggleMusicMuted()"
            >
              <span class="toggle-knob" />
            </button>
          </div>
          <div class="slider-row" :class="{ 'slider-row--disabled': audio.musicMuted }">
            <div class="slider-track-wrapper">
              <div class="slider-track">
                <div class="slider-fill" :style="{ width: musicVolume + '%' }" />
              </div>
              <input
                id="music-volume"
                v-model.number="musicVolume"
                type="range"
                min="0"
                max="100"
                step="1"
                :disabled="audio.musicMuted"
                class="volume-slider"
                aria-label="Musikk volum"
                @input="onMusicInput"
              />
              <div class="slider-thumb" :style="{ left: musicVolume + '%' }" />
            </div>
            <span class="volume-value">{{ musicVolume }}%</span>
          </div>
        </section>

        <!-- SFX -->
        <section class="setting-section">
          <div class="setting-row">
            <div class="setting-label">
              <Volume2 v-if="!audio.sfxMuted" :size="22" class="setting-icon" />
              <VolumeX v-else :size="22" class="setting-icon setting-icon--muted" />
              <label for="sfx-volume">Lydeffekter</label>
            </div>
            <button
              class="toggle-button btn-bare"
              :class="{ 'toggle-button--off': audio.sfxMuted }"
              role="switch"
              :aria-checked="!audio.sfxMuted"
              :aria-label="audio.sfxMuted ? 'Skru på lydeffekter' : 'Demp lydeffekter'"
              @click="audio.toggleSfxMuted()"
            >
              <span class="toggle-knob" />
            </button>
          </div>
          <div class="slider-row" :class="{ 'slider-row--disabled': audio.sfxMuted }">
            <div class="slider-track-wrapper">
              <div class="slider-track">
                <div class="slider-fill" :style="{ width: sfxVolume + '%' }" />
              </div>
              <input
                id="sfx-volume"
                v-model.number="sfxVolume"
                type="range"
                min="0"
                max="100"
                step="1"
                :disabled="audio.sfxMuted"
                class="volume-slider"
                aria-label="Lydeffekter volum"
                @input="onSfxInput"
              />
              <div class="slider-thumb" :style="{ left: sfxVolume + '%' }" />
            </div>
            <span class="volume-value">{{ sfxVolume }}%</span>
          </div>
        </section>

        <section v-if="!auth.isTeacher" class="setting-section">
          <div class="setting-row">
            <div class="setting-label">
              <Zap :size="22" class="setting-icon" />
              <label for="text-speed">Tekstfart</label>
            </div>
            <span class="speed-readout">{{ textSpeed }}/s</span>
          </div>
          <div class="slider-row">
            <div class="slider-track-wrapper">
              <div class="slider-track">
                <div class="slider-fill" :style="{ width: ((textSpeed - 10) / 80 * 100) + '%' }" />
              </div>
              <input
                id="text-speed"
                v-model.number="textSpeed"
                type="range"
                min="10"
                max="90"
                step="1"
                class="volume-slider"
                aria-label="Tekstfart"
                @input="onTextSpeedInput"
              />
              <div class="slider-thumb" :style="{ left: ((textSpeed - 10) / 80 * 100) + '%' }" />
            </div>
          </div>
        </section>

        <!-- Auto-advance -->
        <section v-if="!auth.isTeacher" class="setting-section">
          <div class="setting-row">
            <div class="setting-label">
              <label>Auto-fortsett</label>
            </div>
            <button
              class="toggle-button btn-bare"
              :class="{ 'toggle-button--off': !audio.autoAdvance }"
              role="switch"
              :aria-checked="audio.autoAdvance"
              :aria-label="audio.autoAdvance ? 'Slå av auto-fortsett' : 'Slå på auto-fortsett'"
              @click="audio.toggleAutoAdvance()"
            >
              <span class="toggle-knob" />
            </button>
          </div>
        </section>

        <button class="logout-button btn-bare" @click="logout">
          <LogOut :size="20" />
          Logg ut
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: #faf7f2;
  border-radius: 1rem;
  border: 1px solid #d6d3d1;
  width: 100%;
  max-width: 400px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  color: #44403c;
  margin: 0;
  letter-spacing: 0.06em;
}

.close-button {
  width: 2.25rem;
  height: 2.25rem;
  background: none;
  border: none;
  cursor: pointer;
  color: #78716c;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 0.375rem;
}

.close-button:hover {
  color: #1c1917;
  background: rgba(0, 0, 0, 0.06);
}

.close-button:focus {
  outline: 3px solid #92400e;
  outline-offset: 2px;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.setting-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #d6d3d1;
}

.setting-section:last-of-type {
  border-bottom: none;
  padding-bottom: 0;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.setting-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.setting-label label {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-weight: 700;
  font-size: 1.05rem;
  color: #1c1917;
}

.setting-icon {
  color: #78716c;
  flex-shrink: 0;
}

.setting-icon--muted {
  color: #78716c;
}

.toggle-button {
  position: relative;
  width: 3.25rem;
  height: 1.75rem;
  background: #16a34a;
  border: 2px solid #15803d;
  border-radius: 999px;
  cursor: pointer;
  padding: 0;
  flex-shrink: 0;
  transition: background 0.2s, border-color 0.2s;
}

.toggle-button--off {
  background: #a8a29e;
  border-color: #78716c;
}

.toggle-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 1.2rem;
  height: 1.2rem;
  background: white;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s;
}

.toggle-button:not(.toggle-button--off) .toggle-knob {
  transform: translateX(1.45rem);
}

.toggle-button:focus {
  outline: 3px solid #92400e;
  outline-offset: 2px;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  transition: opacity 0.2s;
}

.slider-row--disabled {
  opacity: 0.5;
  pointer-events: none;
}

.slider-track-wrapper {
  flex: 1;
  position: relative;
  height: 2.5rem;
  display: flex;
  align-items: center;
}

.slider-track {
  position: absolute;
  left: 0;
  right: 0;
  height: 0.5rem;
  background: #d6d3d1;
  border-radius: 999px;
  overflow: hidden;
}

.slider-fill {
  height: 100%;
  background: #92400e;
  border-radius: 999px;
  transition: width 0.05s linear;
}

.slider-thumb {
  position: absolute;
  width: 1.5rem;
  height: 1.5rem;
  background: white;
  border: 3px solid #92400e;
  border-radius: 50%;
  transform: translateX(-50%);
  pointer-events: none;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  transition: left 0.05s linear;
}

.volume-slider {
  position: absolute;
  left: 0;
  right: 0;
  width: 100%;
  opacity: 0;
  cursor: pointer;
  height: 2.5rem;
  margin: 0;
}

.volume-slider:disabled {
  cursor: not-allowed;
}

.volume-slider:focus + .slider-thumb,
.volume-slider:focus ~ .slider-thumb {
  outline: 3px solid #92400e;
  outline-offset: 2px;
}

.volume-value {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 0.95rem;
  font-weight: 700;
  color: #44403c;
  min-width: 2.75rem;
  text-align: right;
}

.speed-readout {
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-weight: 700;
  font-size: 1rem;
  color: #44403c;
}

.logout-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.75rem;
  background: #e7e5e4;
  color: #44403c;
  border: 2px solid #a8a29e;
  border-radius: 0.75rem;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  margin-top: 0.25rem;
}

.logout-button:hover {
  background: #d6d3d1;
}

.logout-button:focus {
  outline: 3px solid #92400e;
  outline-offset: 2px;
}
</style>
