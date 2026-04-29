<script setup lang="ts">
import { ref } from 'vue'
import { useAudioStore } from '@/stores/AudioStore'
import { useAuthStore } from '@/stores/AuthStore'

defineEmits<{ close: [] }>()

const audio = useAudioStore()
const auth = useAuthStore()

const musicVolume = ref(Math.round(audio.musicVolume * 100))
const sfxVolume = ref(Math.round(audio.sfxVolume * 100))
const textSpeed = ref(audio.textSpeed)


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
  <div class="modal-backdrop" role="dialog" aria-modal="true" aria-label="Innstillinger" @click.self="$emit('close')" @keydown.esc="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2 class="modal-title">Innstillinger</h2>
        <button class="close-button btn-bare" aria-label="Lukk innstillinger" @click="$emit('close')">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M4 4L16 16M16 4L4 16" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
        </button>
      </div>

      <div class="modal-body">
        <section class="setting-section">
          <div class="setting-row">
            <label for="music-volume">Musikk</label>
            <button
              class="mute-button btn-bare"
              :aria-label="audio.musicMuted ? 'Skru på musikk' : 'Demp musikk'"
              @click="audio.toggleMusicMuted()"
            >
              {{ audio.musicMuted ? 'Skru på' : 'Skru av' }}
            </button>
          </div>
          <div class="slider-row">
            <div class="slider-track-wrapper" :class="{ 'slider-track-wrapper--disabled': audio.musicMuted }">
              <div class="slider-track">
                <div class="slider-fill" :style="{ width: musicVolume + '%' }" />
              </div>
              <div class="slider-thumb" :style="{ left: musicVolume + '%' }" />
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
            </div>
            <span class="volume-value" :class="{ 'volume-value--muted': audio.musicMuted }">{{ musicVolume }}%</span>
          </div>
        </section>

        <section class="setting-section">
          <div class="setting-row">
            <label for="sfx-volume">Lydeffekter</label>
            <button
              class="mute-button btn-bare"
              :aria-label="audio.sfxMuted ? 'Skru på lydeffekter' : 'Demp lydeffekter'"
              @click="audio.toggleSfxMuted()"
            >
              {{ audio.sfxMuted ? 'Skru på' : 'Skru av' }}
            </button>
          </div>
          <div class="slider-row">
            <div class="slider-track-wrapper" :class="{ 'slider-track-wrapper--disabled': audio.sfxMuted }">
              <div class="slider-track">
                <div class="slider-fill" :style="{ width: sfxVolume + '%' }" />
              </div>
              <div class="slider-thumb" :style="{ left: sfxVolume + '%' }" />
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
            </div>
            <span class="volume-value" :class="{ 'volume-value--muted': audio.sfxMuted }">{{ sfxVolume }}%</span>
          </div>
        </section>

        <section class="setting-section">
          <div class="setting-row">
            <label for="text-speed">Tekstfart</label>
            <span class="speed-readout">{{ textSpeed }}/s</span>
          </div>
          <div class="slider-row">
            <div class="slider-track-wrapper">
              <div class="slider-track">
                <div class="slider-fill" :style="{ width: ((textSpeed - 10) / 80 * 100) + '%' }" />
              </div>
              <div class="slider-thumb" :style="{ left: ((textSpeed - 10) / 80 * 100) + '%' }" />
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
            </div>
          </div>
        </section>

        <section class="setting-section">
          <div class="setting-row">
            <label>Auto-fortsett</label>
            <button
              class="mute-button btn-bare"
              :aria-pressed="audio.autoAdvance"
              @click="audio.toggleAutoAdvance()"
            >
              {{ audio.autoAdvance ? 'På' : 'Av' }}
            </button>
          </div>
        </section>

        <button class="btn-vn btn-vn-danger" @click="logout">Logg ut</button>
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
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 360px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0;
}

.close-button {
  background: none;
  border: none;
  cursor: pointer;
  color: #555;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.close-button:hover {
  color: #000;
  background: #f0f0f0;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.setting-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.setting-row label {
  font-weight: 600;
  font-size: 0.95rem;
}

.mute-button {
  font-size: 0.8rem;
  padding: 2px 10px;
  border: 1px solid #ccc;
  border-radius: 999px;
  background: none;
  cursor: pointer;
  color: inherit;
}

.mute-button:hover {
  background: #f0f0f0;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.slider-track-wrapper {
  flex: 1;
  position: relative;
  height: 28px;
  display: flex;
  align-items: center;
}

.slider-track-wrapper--disabled {
  opacity: 0.4;
}

.slider-track {
  position: absolute;
  left: 0;
  right: 0;
  height: 8px;
  background: #ddd;
  border-radius: 999px;
  overflow: hidden;
}

.slider-fill {
  height: 100%;
  background: var(--color-nd-accent);
  border-radius: 999px;
  transition: width 0.05s linear;
}

.slider-thumb {
  position: absolute;
  width: 20px;
  height: 20px;
  background: white;
  border: 3px solid var(--color-nd-accent);
  border-radius: 50%;
  transform: translateX(-50%);
  pointer-events: none;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
  transition: left 0.05s linear;
}

.volume-slider {
  position: absolute;
  left: 0;
  right: 0;
  width: 100%;
  opacity: 0;
  cursor: pointer;
  height: 28px;
  margin: 0;
}

.volume-slider:disabled {
  cursor: not-allowed;
}

.volume-value {
  font-size: 0.85rem;
  color: #555;
  min-width: 36px;
  text-align: right;
  transition: opacity 0.2s;
}

.volume-value--muted {
  opacity: 0.3;
}

.modal-body > .btn-vn { margin-top: 4px; }

.speed-readout {
  font-weight: 600;
  font-size: 0.95rem;
  color: #555;
}
</style>
