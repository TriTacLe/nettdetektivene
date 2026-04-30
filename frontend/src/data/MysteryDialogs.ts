import type { DialogGraph } from '@/types/cutscene'

export const mysteryStartId = 'mystery-entry'

export const mysteryDialogs: DialogGraph = {
  'mystery-entry': {
    id: 'mystery-entry',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    background: '/CorridorBackground.jpeg',
    lines: [
      { text: 'Sssh... Hold deg lavt. Vi har en ny sak på bordet.', mood: 'sus' },
      { text: 'Jeg trenger en skarp detektiv. Hva tar du for deg i dag?', mood: 'thinking' },
    ],
    choices: [
      { label: 'Avdekk ukens mysterier', next: 'mystery-entry', flag: 'play' },
      { label: 'Send inn et nytt spor', next: 'mystery-entry', flag: 'submit' },
    ],
  },
}

export default mysteryDialogs
