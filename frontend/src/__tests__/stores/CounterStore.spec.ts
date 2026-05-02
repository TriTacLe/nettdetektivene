import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCounterStore } from '../../stores/counter'

describe('CounterStore', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('starts at count 0', () => {
    const store = useCounterStore()
    expect(store.count).toBe(0)
  })

  it('increments count by 1', () => {
    const store = useCounterStore()
    store.increment()
    expect(store.count).toBe(1)
  })

  it('increments multiple times', () => {
    const store = useCounterStore()
    store.increment()
    store.increment()
    store.increment()
    expect(store.count).toBe(3)
  })

  it('computes doubleCount correctly', () => {
    const store = useCounterStore()
    store.increment()
    store.increment()
    expect(store.doubleCount).toBe(4)
  })

  it('doubleCount is 0 initially', () => {
    const store = useCounterStore()
    expect(store.doubleCount).toBe(0)
  })
})
