import type { DialogScene } from '@/types/cutscene'
import { DETECTIVE_IMAGES, DETECTIVE_NAME } from '@/data/characters'

export const fotografenPicker: DialogScene = {
  id: 'fotografen-picker',
  character: DETECTIVE_IMAGES,
  characterName: DETECTIVE_NAME,
  background: '/FotografenBackground.png',
  lines: [
    { text: 'Fotografens studio. Her ser ingenting helt ut som det utgir seg for å være.', mood: 'sus' },
    { text: 'Manipulerte bilder og KI-genererte bilder gjemmer seg blandt oppgavene — øyet ditt må være skarpt.', mood: 'thinking' },
    { text: 'Hva vil du undersøke?', mood: 'neutral' },
  ],
}

export default fotografenPicker
