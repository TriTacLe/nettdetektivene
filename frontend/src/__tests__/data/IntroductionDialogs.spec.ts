import { describe, it, expect } from 'vitest'
import { introductionDialogs, introductionStartId } from '@/data/IntroductionDialogs'
import type { DialogScene } from '@/types/cutscene'

const FINAL_SCENE_ID = 'hacker-intro'

function isQuestionScene(scene: DialogScene): boolean {
  return Boolean(scene.choices && scene.choices.length > 0)
}

function isQuizScene(scene: DialogScene): boolean {
  return scene.id.startsWith('quiz-') && !isWrongScene(scene) && isQuestionScene(scene)
}

function isWrongScene(scene: DialogScene): boolean {
  return scene.id.endsWith('-wrong')
}

describe('introductionDialogs graph', () => {
  it('has the declared start id', () => {
    expect(introductionDialogs[introductionStartId]).toBeDefined()
  })

  it('has the final hacker scene', () => {
    expect(introductionDialogs[FINAL_SCENE_ID]).toBeDefined()
  })

  it('every choice and next pointer references an existing scene', () => {
    for (const scene of Object.values(introductionDialogs)) {
      if (scene.next) {
        expect(introductionDialogs[scene.next], `${scene.id}.next -> ${scene.next}`).toBeDefined()
      }
      for (const choice of scene.choices ?? []) {
        expect(
          introductionDialogs[choice.next],
          `${scene.id} choice "${choice.label}" -> ${choice.next}`,
        ).toBeDefined()
      }
    }
  })

  it('every wrong-answer scene loops back to a question scene', () => {
    const wrongScenes = Object.values(introductionDialogs).filter(isWrongScene)
    expect(wrongScenes.length).toBeGreaterThan(0)

    for (const wrong of wrongScenes) {
      expect(wrong.choices, `${wrong.id} should not present choices`).toBeUndefined()
      expect(wrong.next, `${wrong.id} should chain to a retry target`).toBeDefined()
      const target = introductionDialogs[wrong.next!]
      expect(target, `${wrong.id} -> ${wrong.next}`).toBeDefined()
      expect(isQuestionScene(target), `${wrong.id} should loop back to a question`).toBe(true)
    }
  })

  it('every quiz scene has exactly one option that does not route to a wrong scene', () => {
    const questions = Object.values(introductionDialogs).filter(isQuizScene)
    expect(questions.length).toBeGreaterThan(0)

    for (const question of questions) {
      const correctOptions = (question.choices ?? []).filter(
        (choice) => !isWrongScene(introductionDialogs[choice.next]!),
      )
      expect(correctOptions, `${question.id} must have exactly one correct option`).toHaveLength(1)
    }
  })

  it('picking the correct answer at every question reaches the hacker scene', () => {
    const visited = new Set<string>()
    let currentId: string | null = introductionStartId

    while (currentId) {
      if (visited.has(currentId)) throw new Error(`cycle detected at ${currentId}`)
      visited.add(currentId)
      if (currentId === FINAL_SCENE_ID) break

      const scene: DialogScene | undefined = introductionDialogs[currentId]
      expect(scene, `missing scene ${currentId}`).toBeDefined()

      if (isQuizScene(scene!)) {
        const correct = (scene!.choices ?? []).find(
          (choice) => !isWrongScene(introductionDialogs[choice.next]!),
        )
        expect(correct, `${scene!.id} has no correct option`).toBeDefined()
        currentId = correct!.next
        continue
      }

      // Branching greeting: pick the first option (the "ready" path).
      if (isQuestionScene(scene!)) {
        currentId = scene!.choices![0]!.next
        continue
      }

      // Non-question scenes either chain via `next` or terminate.
      currentId = scene!.next ?? null
    }

    expect(currentId).toBe(FINAL_SCENE_ID)
  })

  it('correct answer index varies across quiz scenes', () => {
    const indexes = Object.values(introductionDialogs)
      .filter(isQuizScene)
      .map((scene) =>
        (scene.choices ?? []).findIndex(
          (choice) => !isWrongScene(introductionDialogs[choice.next]!),
        ),
      )
    expect(new Set(indexes).size).toBeGreaterThan(1)
  })
})
