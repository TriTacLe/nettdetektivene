<template>
  <Transition name="toast">
    <div v-if="show" class="toast-container">
      <div class="toast-message" :class="`toast-${type}`">
        <span class="toast-icon">{{ icon }}</span>
        <span class="toast-text">{{ message }}</span>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  show: boolean
  message: string
  type?: 'success' | 'error' | 'info'
}>()

const icon = computed(() => {
  const icons = {
    success: '✓',
    error: '✕',
    info: 'ℹ'
  }
  return icons[props.type || 'info'] || icons.info
})
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
}

.toast-message {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  font-weight: 600;
  font-size: 1rem;
  min-width: 280px;
  animation: slideIn 0.3s ease-out;
}

.toast-success {
  border-left: 4px solid #22c55e;
}

.toast-error {
  border-left: 4px solid #ef4444;
}

.toast-info {
  border-left: 4px solid #3b82f6;
}

.toast-icon {
  font-size: 1.25rem;
  font-weight: 700;
}

.toast-success .toast-icon {
  color: #22c55e;
}

.toast-error .toast-icon {
  color: #ef4444;
}

.toast-info .toast-icon {
  color: #3b82f6;
}

.toast-text {
  color: #1f2937;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.toast-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>