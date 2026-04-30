import type { DialogGraph } from '@/types/cutscene'

const HACKER_IMG = '/hacker.png'
const DETECTIVE_IMG = '/detective.png'
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
    characterName: 'Hackeren',
    background: BG,
    lines: [
      { text: 'Ssh! Hold kjeft. Du sa du var detektiv, ikke sant?', mood: 'sus' },
      { text: 'Jeg trenger hjelp. Noen jobber mot meg, og jeg vet ikke hvem.', mood: 'thinking' },
      { text: 'Hvis du hjelper meg, lover jeg... vel, jeg lover ingenting. Men det blir spennende.', mood: 'sus' },
    ],
    choices: [
      { label: 'Jeg hjelper deg.', next: 'hacker-shady', flag: 'help' },
      { label: 'La meg vaere i fred.', next: '__end__', flag: 'decline' },
    ],
  },
  'hacker-shady': {
    id: 'hacker-shady',
    character: HACKER_IMG,
    characterName: 'Hackeren',
    background: BG,
    lines: [
      { text: 'Greit. Hor godt etter — dette er stort.', mood: 'sus' },
      { text: 'Jeg planlegger a snike meg inn pa Datasenteret i kveld.', mood: 'angry' },
      { text: 'Hvis noen sporr, har du aldri sett meg. Vi snakkes.', mood: 'sus' },
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
    characterName: 'Detektiv Hansen',
    background: BG,
    lines: [
      { text: 'Du ser ut som du har sett noe. Spytt ut.', mood: 'thinking' },
      { text: 'Hva er det som plager deg?', mood: 'neutral' },
    ],
    choices: [
      { label: 'Jeg traff hackeren. Han planlegger noe pa Datasenteret.', next: 'hacker-caught', flag: 'tell' },
      { label: 'Det er ingenting. Glem det.', next: '__end__', flag: 'silent' },
    ],
  },
  'hacker-caught': {
    id: 'hacker-caught',
    character: HACKER_IMG,
    characterName: 'Hackeren',
    background: BG,
    lines: [
      { text: 'Hva?! Du... du sa det videre?', mood: 'angry' },
      { text: 'Jeg trodde jeg kunne stole pa deg. Pokker ta!', mood: 'angry' },
      { text: 'Greit, greit, jeg gir meg. Du vant denne runden, detektiv.', mood: 'sus' },
    ],
  },
}
