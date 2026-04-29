import { ref, watch, onUnmounted, type Ref } from 'vue'

export interface TypewriterOptions {
  source: Ref<string>
  charsPerSecond?: Ref<number> | number
  onTick?: () => void
}

export function useTypewriter(options: TypewriterOptions) {
  const speedRef = typeof options.charsPerSecond === 'object'
    ? options.charsPerSecond
    : ref(options.charsPerSecond ?? 45)

  const visibleText = ref('')
  const isComplete = ref(false)
  let timer: ReturnType<typeof setTimeout> | null = null
  let cursor = 0

  function clearTimer() {
    if (timer !== null) {
      clearTimeout(timer)
      timer = null
    }
  }

  function step() {
    const full = options.source.value
    if (cursor >= full.length) {
      isComplete.value = true
      return
    }
    cursor++
    visibleText.value = full.slice(0, cursor)
    if (options.onTick && full[cursor - 1] !== ' ') {
      options.onTick()
    }
    const interval = Math.max(8, 1000 / Math.max(1, speedRef.value))
    timer = setTimeout(step, interval)
  }

  function start() {
    clearTimer()
    cursor = 0
    visibleText.value = ''
    isComplete.value = false
    if (options.source.value.length === 0) {
      isComplete.value = true
      return
    }
    step()
  }

  function skip() {
    clearTimer()
    cursor = options.source.value.length
    visibleText.value = options.source.value
    isComplete.value = true
  }

  watch(options.source, () => start(), { immediate: true })

  onUnmounted(clearTimer)

  return { visibleText, isComplete, skip, start }
}
