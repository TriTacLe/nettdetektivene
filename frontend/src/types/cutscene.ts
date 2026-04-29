export type DialogMood = 'neutral' | 'happy' | 'angry' | 'sus' | 'thinking'

export interface DialogLine {
  text: string
  mood?: DialogMood
}

export interface DialogChoice {
  label: string
  next: string
  flag?: string
}

export interface DialogScene {
  id: string
  character: string
  characterName: string
  background?: string
  music?: string
  lines: DialogLine[]
  choices?: DialogChoice[]
  next?: string
}

export type DialogGraph = Record<string, DialogScene>
