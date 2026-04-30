import type { DialogGraph } from '@/types/cutscene'

export const datasenteretOutro: DialogGraph = {
  passed: {
    id: 'passed',
    character: '/hacker.png',
    characterName: 'Hackeren',
    lines: [
      { text: 'Hva?! Det skulle ikke være mulig...', mood: 'angry' },
    ],
  },
  failed: {
    id: 'failed',
    character: '/hacker.png',
    characterName: 'Hackeren',
    lines: [
      { text: 'Hahaha! Ikke godt nok, detektiv.', mood: 'angry' },
    ],
  },
}
