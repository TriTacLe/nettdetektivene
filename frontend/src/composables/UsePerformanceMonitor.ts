import { ref, onUnmounted } from 'vue'

interface PerformanceMetric {
  name: string
  value: number
  timestamp: number
}

export function usePerformanceMonitor() {
  const metrics = ref<PerformanceMetric[]>([])
  const isMonitoring = ref(false)

  function mark(name: string) {
    if (typeof performance !== 'undefined') {
      performance.mark(name)
    }
  }

  function measure(name: string, startMark: string, endMark: string) {
    if (typeof performance !== 'undefined') {
      try {
        const measure = performance.measure(name, startMark, endMark)
        metrics.value.push({
          name,
          value: measure.duration,
          timestamp: Date.now()
        })
        return measure.duration
      } catch (error) {
        console.warn(`Failed to measure ${name}:`, error)
        return 0
      }
    }
    return 0
  }

  function startTiming(name: string) {
    mark(`${name}-start`)
  }

  function endTiming(name: string) {
    const startMark = `${name}-start`
    const endMark = `${name}-end`
    mark(endMark)
    return measure(name, startMark, endMark)
  }

  function getWebVitals() {
    if (typeof performance === 'undefined') return null
    
    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined
    if (!navigation) return null

    return {
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
      firstByte: navigation.responseStart - navigation.requestStart,
      domReady: navigation.domComplete - navigation.domInteractive,
    }
  }

  function getLargestContentfulPaint() {
    if (typeof PerformanceObserver === 'undefined') return null
    
    return new Promise<number>((resolve) => {
      let lcp = 0
      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries()
        const lastEntry = entries[entries.length - 1]
        if (lastEntry) {
          lcp = lastEntry.startTime
          resolve(lcp)
        }
      })
      
      observer.observe({ entryTypes: ['largest-contentful-paint'] })
      
      setTimeout(() => {
        observer.disconnect()
        resolve(lcp)
      }, 2500)
    })
  }

  function recordMetric(name: string, value: number) {
    metrics.value.push({
      name,
      value,
      timestamp: Date.now()
    })
  }

  function getMetrics() {
    return metrics.value
  }

  function clearMetrics() {
    metrics.value = []
  }

  function getMetricsSummary() {
    if (metrics.value.length === 0) return null
    
    const byName = metrics.value.reduce((acc, metric) => {
      if (!acc[metric.name]) {
        acc[metric.name] = []
      }
      const nameArray = acc[metric.name]
      if (nameArray) {
        nameArray.push(metric.value)
      }
      return acc
    }, {} as Record<string, number[]>)

    return Object.entries(byName).map(([name, values]) => {
      const avg = values.reduce((sum, val) => sum + val, 0) / values.length
      const min = Math.min(...values)
      const max = Math.max(...values)
      return { name, avg, min, max, count: values.length }
    })
  }

  onUnmounted(() => {
    if (typeof performance !== 'undefined') {
      performance.clearMarks()
      performance.clearMeasures()
    }
  })

  return {
    metrics,
    isMonitoring,
    mark,
    measure,
    startTiming,
    endTiming,
    getWebVitals,
    getLargestContentfulPaint,
    recordMetric,
    getMetrics,
    clearMetrics,
    getMetricsSummary
  }
}