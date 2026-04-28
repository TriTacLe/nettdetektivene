import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'info'

interface Toast {
  show: boolean
  message: string
  type: ToastType
}

const toast = ref<Toast>({
  show: false,
  message: '',
  type: 'info'
})

let timeoutId: number | null = null

export function useToast() {
  function showToast(message: string, type: ToastType = 'info', duration: number = 3000) {
    toast.value = {
      show: true,
      message,
      type
    }

    if (timeoutId) {
      clearTimeout(timeoutId)
    }

    timeoutId = window.setTimeout(() => {
      toast.value.show = false
    }, duration)
  }

  function showSuccess(message: string) {
    showToast(message, 'success')
  }

  function showError(message: string) {
    showToast(message, 'error')
  }

  function showInfo(message: string) {
    showToast(message, 'info')
  }

  return {
    toast,
    showToast,
    showSuccess,
    showError,
    showInfo
  }
}