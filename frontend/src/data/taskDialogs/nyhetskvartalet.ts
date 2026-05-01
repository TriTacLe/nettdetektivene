import type { DialogGraph } from '@/types/cutscene'

export const nyhetskvartaletOutro: DialogGraph = {
  passed: {
    id: 'passed',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    lines: [
      { text: 'Bra jobbet, detektiv! Saken er løst.', mood: 'happy' },
    ],
  },
  failed: {
    id: 'failed',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    lines: [
      { text: 'Tett på, men ikke helt. Tenk gjennom hva som skilte den falske artikkelen ut.', mood: 'thinking' },
    ],
  },
}
