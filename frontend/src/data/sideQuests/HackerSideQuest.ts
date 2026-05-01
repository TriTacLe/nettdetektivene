import type { SideQuestDef } from '@/data/sideQuests/types'
import { hackerStage0, hackerStage1 } from '@/data/sideQuests/hackerScenes'

/**
 * Two-visit branching arc.
 *
 *   stage 0 (visit 1) — meet hacker → choose:
 *      help   → hear his plan (same visit), advance to stage 1
 *      decline → abandoned, no achievement
 *   stage 1 (visit 2) — detective debrief → choose:
 *      tell   → confront hacker (same visit), completed + caught_hacker
 *      silent → abandoned, no achievement
 */
export const hackerSideQuest: SideQuestDef = {
  key: 'hacker',
  stages: {
    0: {
      graph: hackerStage0,
      startId: 'hacker-meet',
      onChoose: (choice) => {
        if (choice.flag === 'decline') return { kind: 'abandon' }
        return null
      },
      onDone: () => ({ kind: 'advance', nextStage: 1 }),
    },
    1: {
      graph: hackerStage1,
      startId: 'detective-debrief',
      onChoose: (choice) => {
        if (choice.flag === 'silent') return { kind: 'abandon' }
        return null
      },
      onDone: () => ({ kind: 'complete', achievementRef: 'caught_hacker' }),
    },
  },
}
