import type { DialogGraph } from '@/types/cutscene'
import { DETECTIVE_IMAGES, DETECTIVE_NAME } from './characters'

export const mysteryStartId = 'mystery-entry'

export const mysteryDialogs: DialogGraph = {
  'mystery-entry': {
    id: 'mystery-entry',
    character: DETECTIVE_IMAGES,
    characterName: DETECTIVE_NAME,
    background: '/CorridorBackground.jpeg',
    lines: [
      { text: 'Sssh... Hold deg lavt. Vi har en ny sak på bordet.', mood: 'sus' },
      { text: 'Jeg trenger en skarp detektiv. Hva tar du for deg i dag?', mood: 'thinking' },
    ],
    // The parent view reads `flag` from the @choose handler and navigates
    // away before the runner advances, so `next` is only used as a render key.
    choices: [
      { label: 'Avdekk ukens mysterier', next: 'mystery-entry', flag: 'play' },
      { label: 'Send inn et nytt spor', next: 'mystery-entry', flag: 'submit' },
    ],
  },
}

export default mysteryDialogs
