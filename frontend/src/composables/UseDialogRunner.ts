import { computed, ref, type Ref } from 'vue'
import type { DialogChoice, DialogGraph, DialogScene } from '@/types/cutscene'

export interface DialogRunner {
  currentScene: Ref<DialogScene | null>
  isFinished: Ref<boolean>
  flags: Ref<Set<string>>
  advance: (choice?: DialogChoice) => void
  reset: (startId?: string) => void
}

export function useDialogRunner(graph: DialogGraph, startId: string): DialogRunner {
  const currentId = ref<string | null>(startId)
  const flags = ref<Set<string>>(new Set())

  const currentScene = computed<DialogScene | null>(() => {
    if (!currentId.value) return null
    const scene = graph[currentId.value]
    return scene ?? null
  })

  const isFinished = computed(() => currentId.value === null)

  function advance(choice?: DialogChoice) {
    const scene = currentScene.value
    if (!scene) return

    if (choice) {
      if (choice.flag) flags.value.add(choice.flag)
      currentId.value = graph[choice.next] ? choice.next : null
      return
    }

    if (scene.choices && scene.choices.length > 0) {
      // Caller must pass a choice when one is required.
      return
    }

    if (scene.next && graph[scene.next]) {
      currentId.value = scene.next
      return
    }

    currentId.value = null
  }

  function reset(newStartId?: string) {
    currentId.value = newStartId ?? startId
    flags.value = new Set()
  }

  return { currentScene, isFinished, flags, advance, reset }
}
