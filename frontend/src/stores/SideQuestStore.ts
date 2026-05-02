import { defineStore } from 'pinia'
import { ref } from 'vue'
import SideQuestService, {
  type SideQuestState,
  type SideQuestStatus,
} from '@/services/SideQuestService'

export const useSideQuestStore = defineStore('sideQuest', () => {
  const state = ref<Map<string, SideQuestState>>(new Map())
  const loaded = ref(false)

  function applyEntry(entry: SideQuestState) {
    state.value.set(entry.key, entry)
  }

  async function load(): Promise<void> {
    try {
      const list = await SideQuestService.getMine()
      const next = new Map<string, SideQuestState>()
      for (const entry of list) next.set(entry.key, entry)
      state.value = next
    } catch {
      // Leave whatever we had — quests just don't trigger until next load.
    } finally {
      loaded.value = true
    }
  }

  function clear(): void {
    state.value = new Map()
    loaded.value = false
  }

  function get(key: string): SideQuestState | undefined {
    return state.value.get(key)
  }

  function getStage(key: string): number {
    return state.value.get(key)?.stage ?? 0
  }

  function getStatus(key: string): SideQuestStatus | null {
    return state.value.get(key)?.status ?? null
  }

  function isCompleted(key: string): boolean {
    return state.value.get(key)?.status === 'COMPLETED'
  }

  function isAbandoned(key: string): boolean {
    return state.value.get(key)?.status === 'ABANDONED'
  }

  function isActive(key: string): boolean {
    return state.value.get(key)?.status === 'ACTIVE'
  }

  /**
   * A quest is eligible if it has either never been started, or is currently
   * ACTIVE. Once it's COMPLETED or ABANDONED it's permanently out.
   */
  function isEligible(key: string): boolean {
    const entry = state.value.get(key)
    if (!entry) return true
    return entry.status === 'ACTIVE'
  }

  async function upsert(key: string, stage: number, status: SideQuestStatus): Promise<void> {
    // Optimistic local write so the next mount roll sees fresh state without
    // waiting for the round-trip.
    applyEntry({ key, stage, status })
    try {
      const updated = await SideQuestService.upsert(key, stage, status)
      applyEntry(updated)
    } catch {
      // Optimistic update stands; will reconcile on next load.
    }
  }

  async function advance(key: string, stage: number): Promise<void> {
    return upsert(key, stage, 'ACTIVE')
  }

  async function complete(key: string, stage: number): Promise<void> {
    return upsert(key, stage, 'COMPLETED')
  }

  async function abandon(key: string, stage: number): Promise<void> {
    return upsert(key, stage, 'ABANDONED')
  }

  return {
    state,
    loaded,
    load,
    clear,
    get,
    getStage,
    getStatus,
    isCompleted,
    isAbandoned,
    isActive,
    isEligible,
    advance,
    complete,
    abandon,
  }
})
