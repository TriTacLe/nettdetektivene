import type { SideQuestDef } from '@/data/sideQuests/types'
import { quizGraph } from '@/data/sideQuests/quizScenes'

/**
 * Single-visit quiz. The runner walks the question graph until either
 * `quiz-wrong` (any wrong answer) or `quiz-perfect` (every answer correct)
 * terminates it. The runner's flag set tells us which terminal we reached.
 */
export const detectiveQuizSideQuest: SideQuestDef = {
  key: 'detective_quiz',
  stages: {
    0: {
      graph: quizGraph,
      startId: 'quiz-intro',
      onDone: (flags) => {
        if (flags.has('wrong')) return { kind: 'abandon' }
        return { kind: 'complete', achievementRef: 'quiz_perfect' }
      },
    },
  },
}
