import type { DialogScene } from '@/types/cutscene'
import { DETECTIVE_IMAGES, DETECTIVE_NAME } from '@/data/characters'

export const datasenteretPicker: DialogScene = {
  id: 'datasenteret-picker',
  character: DETECTIVE_IMAGES,
  characterName: DETECTIVE_NAME,
  background: '/datasenteret.jpg',
  lines: [
    { text: 'Datasenteret. Vi er i selve hjertet av nettet nå — og motstanderen vet at vi er her.', mood: 'sus' },
    { text: 'Alt du har lært skal brukes her inne. Det finnes ingen snarveier her.', mood: 'thinking' },
    { text: 'Velg din neste utfordring, detektiv.', mood: 'neutral' },
  ],
}

export default datasenteretPicker
