import type { DialogScene } from '@/types/cutscene'

export const nyhetskvartaletPicker: DialogScene = {
  id: 'nyhetskvartalet-picker',
  character: '/detective.png',
  characterName: 'Detektiv Hansen',
  background: '/NyhetskvartaletBackground.jpg',
  lines: [
    { text: 'Velkommen til Nyhetskvartalet, detektiv. Her er det masse det av overskrifter — og ikke alle er like ærlige.', mood: 'neutral' },
    { text: 'Noen av artiklene er laget for å lure folk. Det er vår jobb å finne de falske nyhetene.', mood: 'sus' },
    { text: 'Hvilken sak vil du ta fatt på først?', mood: 'thinking' },
  ],
}

export default nyhetskvartaletPicker
