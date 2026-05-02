import type { DialogGraph } from '@/types/cutscene'
import { DETECTIVE_IMAGES, DETECTIVE_NAME } from '@/data/characters'

export const fotografenOutro: DialogGraph = {
  passed: {
    id: 'passed',
    character: DETECTIVE_IMAGES,
    characterName: DETECTIVE_NAME,
    lines: [
      { text: 'Skarpt øye, detektiv! Du så det ingen andre så.', mood: 'happy' },
    ],
  },
  failed: {
    id: 'failed',
    character: DETECTIVE_IMAGES,
    characterName: DETECTIVE_NAME,
    lines: [
      { text: 'Nei, det var ikke helt. Bilder er vanskeligere enn de ser ut. Prøv igjen.', mood: 'sad' },
    ],
  },
}
