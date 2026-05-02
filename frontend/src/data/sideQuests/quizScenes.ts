import type { DialogGraph } from '@/types/cutscene'
import { DETECTIVE_IMAGES, DETECTIVE_NAME } from '@/data/characters'

const DETECTIVE_IMG = DETECTIVE_IMAGES
const BG = '/StartBackground.jpg'

/**
 * Three short, kid-friendly questions (3.–7. klasse). Topics map straight to
 * what the player meets in the game: ekte vs. falske nyheter
 * (Nyhetskvartalet) and ekte / AI-generert / manipulerte bilder (Fotografen).
 *
 * The correct option's index is varied per question (q1=2, q2=0, q3=1) so a
 * positional pattern can't be learned across questions.
 *
 * Wrong answers route to `quiz-wrong` → quest is abandoned. The last
 * correct answer routes to `quiz-perfect` → quest completed + medal.
 */
export const quizGraph: DialogGraph = {
  'quiz-intro': {
    id: 'quiz-intro',
    character: DETECTIVE_IMG,
    characterName: DETECTIVE_NAME,
    background: BG,
    lines: [
      { text: 'Hei, detektiv! Klar for en mini-kviss?', mood: 'happy' },
      { text: 'Tre korte spørsmål. Klarer du alle, får du en medalje!', mood: 'thinking' },
    ],
    next: 'quiz-q1',
  },

  'quiz-q1': {
    id: 'quiz-q1',
    character: DETECTIVE_IMG,
    characterName: DETECTIVE_NAME,
    background: BG,
    lines: [{ text: 'Hva er en falsk nyhet?', mood: 'thinking' }],
    choices: [
      { label: 'En morsom historie som er sann.', next: 'quiz-wrong', flag: 'wrong' },
      { label: 'En nyhet skrevet av en kjent journalist.', next: 'quiz-wrong', flag: 'wrong' },
      { label: 'En nyhet som er funnet på eller ikke stemmer.', next: 'quiz-q2', flag: 'correct' },
    ],
  },

  'quiz-q2': {
    id: 'quiz-q2',
    character: DETECTIVE_IMG,
    characterName: DETECTIVE_NAME,
    background: BG,
    lines: [{ text: 'Hva betyr at et bilde er "KI-generert"?', mood: 'thinking' }],
    choices: [
      { label: 'En datamaskin har laget bildet.', next: 'quiz-q3', flag: 'correct' },
      { label: 'Bildet er tatt med mobilkamera.', next: 'quiz-wrong', flag: 'wrong' },
      { label: 'Bildet er trykket i en avis.', next: 'quiz-wrong', flag: 'wrong' },
    ],
  },

  'quiz-q3': {
    id: 'quiz-q3',
    character: DETECTIVE_IMG,
    characterName: DETECTIVE_NAME,
    background: BG,
    lines: [{ text: 'Hva bør du gjøre hvis du er usikker på om en nyhet er ekte?', mood: 'thinking' }],
    choices: [
      { label: 'Dele den med vennene mine med en gang.', next: 'quiz-wrong', flag: 'wrong' },
      { label: 'Sjekke om flere aviser skriver om det samme.', next: 'quiz-perfect', flag: 'correct' },
      { label: 'Tro på den hvis overskriften er kul.', next: 'quiz-wrong', flag: 'wrong' },
    ],
  },

  'quiz-wrong': {
    id: 'quiz-wrong',
    character: DETECTIVE_IMG,
    characterName: DETECTIVE_NAME,
    background: BG,
    lines: [
      { text: 'Hmm, det stemte ikke helt.', mood: 'sad' },
      { text: 'Bedre lykke neste gang, detektiv!', mood: 'neutral' },
    ],
  },

  'quiz-perfect': {
    id: 'quiz-perfect',
    character: DETECTIVE_IMG,
    characterName: DETECTIVE_NAME,
    background: BG,
    lines: [
      { text: 'Riktig på alle! Skarpe øyne, du!', mood: 'happy' },
      { text: 'Her er medaljen du fortjente.', mood: 'happy' },
    ],
  },
}
