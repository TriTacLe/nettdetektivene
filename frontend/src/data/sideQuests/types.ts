import type { DialogChoice, DialogGraph } from '@/types/cutscene'

/** Result of a stage handler — what the runner should do once the dialog ends. */
export type SideQuestOutcome =
  | { kind: 'advance'; nextStage: number }
  | { kind: 'complete'; achievementRef?: string }
  | { kind: 'abandon' }
  | { kind: 'noop' }

export interface SideQuestStageDef {
  graph: DialogGraph
  startId: string
  /** Called when the user picks a choice (whether or not it ends the runner). */
  onChoose?: (choice: DialogChoice, flags: Set<string>) => SideQuestOutcome | null
  /** Called when the runner runs out of scenes (no `next`, no `choices`). */
  onDone?: (flags: Set<string>) => SideQuestOutcome
}

export interface SideQuestDef {
  /** Stable backend key for this quest's `user_side_quest.quest_key`. */
  key: string
  /** Stages keyed by integer; missing stages mean the quest has nothing more to play. */
  stages: Record<number, SideQuestStageDef>
}
