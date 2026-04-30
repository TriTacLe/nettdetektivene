import type { DialogScene } from '@/types/cutscene'

export const secretHackerScene: DialogScene = {
  id: 'secret-hacker',
  character: '/hacker.png',
  characterName: 'Hackeren',
  background: '/StartBackground.jpg',
  lines: [
    { text: 'Hva?! Jeg var akkurat i ferd med noe... lyssky.', mood: 'sus' },
    { text: 'Jeg trodde ingen så meg! Hvordan klarte du å snike deg innpå meg?', mood: 'angry' },
    { text: 'Jeg stikker — du har ikke sett meg her, hører du?!', mood: 'angry' },
  ],
}

export default secretHackerScene
