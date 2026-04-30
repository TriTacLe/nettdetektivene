import type { DialogGraph } from '@/types/cutscene'

export const fotografenOutro: DialogGraph = {
  passed: {
    id: 'passed',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    lines: [
      { text: 'Skarpt øye, detektiv! Du så det ingen andre så.', mood: 'happy' },
    ],
  },
  failed: {
    id: 'failed',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    lines: [
      { text: 'Nei, det var ikke helt. Bilder er trickier enn de ser ut. Prøv igjen.', mood: 'thinking' },
    ],
  },
}
