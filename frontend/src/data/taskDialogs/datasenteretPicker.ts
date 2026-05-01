import type { DialogScene } from '@/types/cutscene'

export const datasenteretPicker: DialogScene = {
  id: 'datasenteret-picker',
  character: '/detective.png',
  characterName: 'Detektiv Hansen',
  background: '/datasenteret.jpg',
  lines: [
    { text: 'Datasenteret. Vi er i selve hjertet av nettet nå — og motstanderen vet at vi er her.', mood: 'sus' },
    { text: 'Alt du har lært skal brukes her inne. Det finnes ingen snarveier her.', mood: 'thinking' },
    { text: 'Velg din neste utfordring, detektiv.', mood: 'neutral' },
  ],
}

export default datasenteretPicker
