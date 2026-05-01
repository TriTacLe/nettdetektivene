import type { SideQuestDef } from '@/data/sideQuests/types'
import { hackerSideQuest } from '@/data/sideQuests/HackerSideQuest'
import { detectiveQuizSideQuest } from '@/data/sideQuests/DetectiveQuizSideQuest'

/** Registry of all side quests eligible to roll on `/game-map` mount. */
export const SIDE_QUESTS: SideQuestDef[] = [
  hackerSideQuest,
  detectiveQuizSideQuest,
]

export type { SideQuestDef, SideQuestStageDef, SideQuestOutcome } from '@/data/sideQuests/types'
