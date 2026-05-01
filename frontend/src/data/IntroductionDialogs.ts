import type { DialogGraph } from '@/types/cutscene'

export const introductionStartId = 'detective-intro'

export const introductionDialogs: DialogGraph = {
  'detective-intro': {
    id: 'detective-intro',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    lines: [
      { text: 'Hei! Jeg er Detektiv Hansen, og jeg trenger din hjelp.', mood: 'neutral' },
      { text: 'Internettbyen er under angrep. Noen sprer løgner,og lurer folk med falske bilder.', mood: 'sus' },
      { text: 'Din jobb er å bli en nettdetektiv. Du skal løse oppgaver og finne spor', mood: 'neutral' },
      { text: 'Er du klar for oppdraget?', mood: 'thinking' },
    ],
    choices: [
      { label: 'Ja, jeg er klar!', next: 'detective-confirm', flag: 'eager' },
      { label: 'Fortell meg litt mer først.', next: 'detective-explain' },
    ],
  },
  'detective-confirm': {
    id: 'detective-confirm',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    lines: [
      { text: 'Akkurat det jeg ville høre. Følg med — du møter snart fienden.', mood: 'happy' },
    ],
    next: 'hacker-intro',
  },
  'detective-explain': {
    id: 'detective-explain',
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    lines: [
      { text: 'Du skal løse små saker rundt om i byen. Hver sak lærer deg noe nytt om hvordan man kan bli lurt på nett.', mood: 'neutral' },
      { text: 'Når du har trent nok, kan vi ta hovedmistenkten sammen. Klar?', mood: 'thinking' },
    ],
    next: 'hacker-intro',
  },
  'hacker-intro': {
    id: 'hacker-intro',
    character: '/hacker.png',
    characterName: 'Hackeren',
    lines: [
      { text: 'Ha! Så du tror du kan stoppe meg?', mood: 'angry' },
      { text: 'Jeg har spredt falske nyheter over hele internett. Folk tror på alt jeg sier!', mood: 'angry' },
      { text: 'Jeg lager falske bilder, og ingen vet hva som er sant eller usant lenger.', mood: 'sus' },
      { text: 'La oss se om en liten nettdetektiv kan avsløre meg... Jeg tviler sterkt!', mood: 'angry' },
    ],
  },
}

export default introductionDialogs
