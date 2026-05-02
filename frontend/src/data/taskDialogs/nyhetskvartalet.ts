import type { DialogGraph } from '@/types/cutscene'
import { DETECTIVE_IMAGES, DETECTIVE_NAME } from '@/data/characters'

export const nyhetskvartaletOutro: DialogGraph = {
  passed: {
    id: 'passed',
    character: DETECTIVE_IMAGES,
    characterName: DETECTIVE_NAME,
    background: '/NyhetskvartaletBackground.jpg',
    lines: [
      { text: 'Bra jobbet, detektiv! Saken er løst.', mood: 'happy' },
      { text: 'Klar for neste oppdrag?', mood: 'happy' },
    ],
  },
  failed: {
    id: 'failed',
    character: DETECTIVE_IMAGES,
    characterName: DETECTIVE_NAME,
    background: '/NyhetskvartaletBackground.jpg',
    lines: [
      { text: 'Tett på, men ikke helt. Tenk gjennom hva som skilte den falske artikkelen ut.', mood: 'sad' },
    ],
  },
}
