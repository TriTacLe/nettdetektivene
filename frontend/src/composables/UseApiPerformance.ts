import { computed, onMounted, onUnmounted, ref } from 'vue'

export function useApiPerformance() {
  const apiCalls = ref<Array<{
    url: string
    method: string
    duration: number
    status: number
    timestamp: number
  }>>([])

  const slowCalls = computed(() => 
    apiCalls.value.filter(call => call.duration > 1000)
  )

  const failedCalls = computed(() => 
    apiCalls.value.filter(call => call.status >= 400)
  )

  const averageDuration = computed(() => {
    if (apiCalls.value.length === 0) return 0
    const total = apiCalls.value.reduce((sum, call) => sum + call.duration, 0)
    return total / apiCalls.value.length
  })

  function recordApiCall(url: string, method: string, duration: number, status: number) {
    apiCalls.value.push({
      url,
      method,
      duration,
      status,
      timestamp: Date.now()
    })

    if (duration > 2000) {
      console.warn(`Slow API call: ${method} ${url} took ${duration}ms`)
    }
  }

  function getPerformanceReport() {
    return {
      totalCalls: apiCalls.value.length,
      slowCalls: slowCalls.value.length,
      failedCalls: failedCalls.value.length,
      averageDuration: averageDuration.value,
      byMethod: apiCalls.value.reduce((acc, call) => {
        if (!acc[call.method]) {
          acc[call.method] = { count: 0, totalDuration: 0 }
        }
        const methodData = acc[call.method]
        if (methodData) {
          methodData.count++
          methodData.totalDuration += call.duration
        }
        return acc
      }, {} as Record<string, { count: number; totalDuration: number }>)
    }
  }

  function clear() {
    apiCalls.value = []
  }

  return {
    apiCalls,
    slowCalls,
    failedCalls,
    averageDuration,
    recordApiCall,
    getPerformanceReport,
    clear
  }
}