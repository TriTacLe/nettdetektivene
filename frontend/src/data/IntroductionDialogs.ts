import type { DialogScene } from '@/types/cutscene'

const introductionDialogs: DialogScene[] = [
  {
    character: '/detective.png',
    characterName: 'Detektiv Hansen',
    lines: [
      'Hei! Jeg er Detektiv Hansen, og jeg trenger din hjelp.',
      'Internettbyen er under angrep. Noen sprer løgner, stjeler passord og lurer folk med falske bilder.',
      'Din jobb er å bli en nettdetektiv. Du skal løse oppgaver, finne spor og tenke kritisk.',
      'Klar deg godt, så klarer vi å beskytte byen sammen!',
    ],
  },
  {
    character: '/hacker.png',
    characterName: 'Hackeren',
    lines: [
      'Ha! Så du tror du kan stoppe meg?',
      'Jeg har allerede spredt falske nyheter over hele internett. Folk tror på alt jeg sier!',
      'Jeg stjeler passord, lager falske bilder og sender løgnaktige e-poster. Ingen aner at det er løgn.',
      'La oss se om en liten nettdetektiv kan avsløre meg... Jeg tviler sterkt!',
    ],
  },
]

export default introductionDialogs
