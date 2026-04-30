import type { DialogScene } from '@/types/cutscene'

export const fotografenPicker: DialogScene = {
  id: 'fotografen-picker',
  character: '/detective.png',
  characterName: 'Detektiv Hansen',
  background: '/FotografenBackground.png',
  lines: [
    { text: 'Fotografens studio. Her ser ingenting helt ut som det utgir seg for å være.', mood: 'sus' },
    { text: 'Manipulerte bilder, KI-genererte ansikter, retusjerte spor — øyet ditt må være skarpt.', mood: 'thinking' },
    { text: 'Hva vil du undersøke?', mood: 'neutral' },
  ],
}

export default fotografenPicker
