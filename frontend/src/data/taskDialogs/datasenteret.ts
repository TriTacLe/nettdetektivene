import type { DialogGraph } from '@/types/cutscene'
import { HACKER_IMAGE, HACKER_NAME } from '@/data/characters'

export const datasenteretOutro: DialogGraph = {
  passed: {
    id: 'passed',
    character: HACKER_IMAGE,
    characterName: HACKER_NAME,
    background: '/datasenteret.jpg',
    lines: [
      { text: 'Hva?! Det skulle ikke være mulig...', mood: 'angry' },
    ],
  },
  failed: {
    id: 'failed',
    character: HACKER_IMAGE,
    characterName: HACKER_NAME,
    background: '/datasenteret.jpg',
    lines: [
      { text: 'Hahaha! Ikke godt nok, detektiv.', mood: 'angry' },
    ],
  },
}
