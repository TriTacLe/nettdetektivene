import type { DialogGraph } from '@/types/cutscene'
import { DETECTIVE_IMAGES, DETECTIVE_NAME, HACKER_IMAGE, HACKER_NAME } from '@/data/characters'

const HACKER_IMG = HACKER_IMAGE
const DETECTIVE_IMG = DETECTIVE_IMAGES
const BG = '/StartBackground.jpg'

/**
 * Stage 0 — Visit 1.
 *   Meet hacker → choose:
 *     "help"    → he reveals his shady plan in the same visit, ends with advance
 *     "decline" → he shrugs and disappears, ends with abandon
 */
export const hackerStage0: DialogGraph = {
  'hacker-meet': {
    id: 'hacker-meet',
    character: HACKER_IMG,
    characterName: HACKER_NAME,
    background: BG,
    lines: [
      { text: 'Ssh! Du sa du var detektiv, ikke sant?', mood: 'sus' },
      { text: 'Jeg trenger hjelp. Noen jobber mot meg, og jeg vet ikke hvem.', mood: 'thinking' },
      { text: 'Hvis du hjelper meg, lover jeg... vel, jeg lover ingenting. Men det blir spennende.', mood: 'sus' },
    ],
    choices: [
      { label: 'Jeg hjelper deg.', next: 'hacker-shady', flag: 'help' },
      { label: 'La meg være i fred.', next: '__end__', flag: 'decline' },
    ],
  },
  'hacker-shady': {
    id: 'hacker-shady',
    character: HACKER_IMG,
    characterName: HACKER_NAME,
    background: BG,
    lines: [
      { text: 'Greit. Hør godt etter — dette er stort.', mood: 'sus' },
      { text: 'Jeg planlegger a snike meg inn pa Datasenteret i kveld.', mood: 'angry' },
      { text: 'Hvis noen spørr, har du aldri sett meg. Vi snakkes.', mood: 'sus' },
    ],
  },
}

/**
 * Stage 1 — Visit 2.
 *   Detective debriefs the player → choose:
 *     "tell"   → cut to the hacker getting cornered, completes + achievement
 *     "silent" → detective shrugs, abandons (no achievement)
 */
export const hackerStage1: DialogGraph = {
  'detective-debrief': {
    id: 'detective-debrief',
    character: DETECTIVE_IMG,
    characterName: DETECTIVE_NAME,
    background: BG,
    lines: [
      { text: 'Du ser ut som du har sett noe. Spytt ut.', mood: 'sus' },
      { text: 'Hva er det som plager deg?', mood: 'thinking' },
    ],
    choices: [
      { label: 'Jeg traff hackeren. Han planlegger noe pa Datasenteret.', next: 'hacker-caught', flag: 'tell' },
      { label: 'Det er ingenting. Glem det.', next: '__end__', flag: 'silent' },
    ],
  },
  'hacker-caught': {
    id: 'hacker-caught',
    character: HACKER_IMG,
    characterName: HACKER_NAME,
    background: BG,
    lines: [
      { text: 'Hva?! Du... du sa det videre?', mood: 'angry' },
      { text: 'Jeg trodde jeg kunne stole pa deg. Pokker ta!', mood: 'angry' },
      { text: 'Greit, greit, jeg gir meg. Du vant denne runden, detektiv.', mood: 'sus' },
    ],
  },
}
