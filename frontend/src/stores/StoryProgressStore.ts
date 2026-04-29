import { defineStore } from 'pinia'
import { ref } from 'vue'
import StoryProgressService from '@/services/StoryProgressService'

export const StoryStep = {
  INTRODUCTION: 1,
  TASK_INTRO_NYHETSKVARTALET: 2,
  TASK_INTRO_FOTOGRAFEN: 3,
  TASK_INTRO_DATASENTERET: 4,
  ENDING: 5,
} as const

export type StoryStepValue = typeof StoryStep[keyof typeof StoryStep]

export const useStoryProgressStore = defineStore('storyProgress', () => {
  const step = ref(0)
  const loaded = ref(false)

  async function load(): Promise<void> {
    try {
      step.value = await StoryProgressService.getMine()
    } catch {
      // Leave at 0; dialogs default to "not seen" until next load.
    } finally {
      loaded.value = true
    }
  }

  function clear(): void {
    step.value = 0
    loaded.value = false
  }

  function hasSeen(target: StoryStepValue): boolean {
    return step.value >= target
  }

  async function advance(target: StoryStepValue): Promise<void> {
    if (step.value >= target) return
    step.value = target
    try {
      const updated = await StoryProgressService.advance(target)
      if (updated > step.value) step.value = updated
    } catch {
      // Optimistic — local value already advanced; will reconcile on next load.
    }
  }

  return { step, loaded, load, clear, hasSeen, advance }
})
