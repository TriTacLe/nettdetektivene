import type { DialogGraph } from '@/types/cutscene'

export const fotografenIntro: DialogGraph = {
  start: {
    id: 'start',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    lines: [
      { text: 'Bilder kan lyve. Her må du skille ekte foto fra AI-bilder og manipulasjoner.', mood: 'sus' },
      { text: 'Se etter rare detaljer — skygger, fingre, tekstur. Klar?', mood: 'thinking' },
    ],
  },
}

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
