import type { DialogGraph } from '@/types/cutscene'

export const datasenteretIntro: DialogGraph = {
  start: {
    id: 'start',
    character: '/hacker.png',
    characterName: 'Hackeren',
    lines: [
      { text: 'Velkommen til datasenteret, lille detektiv. La oss se om du klarer å lese koden min.', mood: 'angry' },
    ],
  },
}

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
