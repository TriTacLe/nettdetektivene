import type { DialogGraph } from '@/types/cutscene'

export const endingStartId = 'detective-victory'

export const endingDialogs: DialogGraph = {
  'detective-victory': {
    id: 'detective-victory',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    background: '/datasenteret.jpg',
    lines: [
      { text: 'Du klarte det! Datatyven har ingen flere triks igjen.', mood: 'happy' },
      { text: 'Hele Internettbyen er trygg takket være deg.', mood: 'happy' },
      { text: 'Skal vi rapportere ham til politiet med en gang?', mood: 'thinking' },
    ],
    choices: [
      { label: 'Ja, ring politiet.', next: 'detective-call', flag: 'no-mercy' },
      { label: 'La oss høre hva han har å si først.', next: 'hacker-broken' },
    ],
  },
  'detective-call': {
    id: 'detective-call',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    background: '/datasenteret.jpg',
    lines: [
      { text: 'Hallo, politiet? Vi har tatt datatyven på fersken her i datasenteret.', mood: 'neutral' },
      { text: 'Send en patrulje. Jeg holder ham her til dere kommer.', mood: 'neutral' },
    ],
    next: 'hacker-broken',
  },
  'hacker-broken': {
    id: 'hacker-broken',
    character: '/hacker.png',
    characterName: 'Hackeren',
    background: '/datasenteret.jpg',
    lines: [
      { text: 'Nei... nei nei nei. Hvordan klarte du det?', mood: 'angry' },
      { text: 'Ingen har avslørt meg før. Ingen!', mood: 'angry' },
      { text: '...', mood: 'sus' },
    ],
    next: 'hacker-regret',
  },
  'hacker-regret': {
    id: 'hacker-regret',
    character: '/hacker.png',
    characterName: 'Hackeren',
    background: '/datasenteret.jpg',
    lines: [
      { text: 'Kanskje... kanskje jeg har gjort feil ting.', mood: 'thinking' },
      { text: 'Folk har mistet penger. Folk har mistet tilliten sin.', mood: 'sus' },
      { text: 'Jeg er lei meg, detektiv. Jeg fortjener det som kommer.', mood: 'thinking' },
    ],
    next: 'detective-closing',
  },
  'detective-closing': {
    id: 'detective-closing',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    background: '/datasenteret.jpg',
    lines: [
      { text: 'Politiet er på vei. De skal ta over herfra.', mood: 'neutral' },
      { text: 'Du er ikke detektiv-i-opplæring lenger. Du er en ekte nettdetektiv nå.', mood: 'happy' },
      { text: 'Tusen takk for hjelpen. Internettbyen står i takk til deg.', mood: 'happy' },
    ],
  },
}

export default endingDialogs
