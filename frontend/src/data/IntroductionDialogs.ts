import type { DialogGraph } from '@/types/cutscene'

export const introductionStartId = 'detective-intro'

const DETECTIVE = '/detective.png'
const DETECTIVE_NAME = 'Detektiv Hansen'

const FEATURE_MAP = '/MapUpdated.png'
const FEATURE_MYSTERY = '/mystery-magnifier.png'
const FEATURE_ACHIEVEMENTS = '/unlockedBadge.png'
const FEATURE_NOTEBOOK = '/notebook.jpg'

export const introductionDialogs: DialogGraph = {
  'detective-intro': {
    id: 'detective-intro',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    lines: [
      { text: 'Hei! Jeg er Detektiv Hansen, og jeg trenger din hjelp.', mood: 'neutral' },
      { text: 'Internettbyen er under angrep. Noen sprer løgner, og lurer folk med falske bilder.', mood: 'sus' },
      { text: 'Jobben din blir å bli en nettdetektiv som avslører løgnene og finner sporene.', mood: 'neutral' },
      { text: 'Før vi setter i gang, vil jeg sjekke at du har skjønt oppdraget. Klar for noen raske spørsmål?', mood: 'thinking' },
    ],
    choices: [
      { label: 'Ja, jeg er klar!', next: 'quiz-mission', flag: 'eager' },
      { label: 'Fortell meg litt mer først.', next: 'detective-explain' },
    ],
  },

  'detective-explain': {
    id: 'detective-explain',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    lines: [
      { text: 'Du skal løse små saker rundt om i byen. Hver sak lærer deg noe nytt om hvordan man kan bli lurt på nett.', mood: 'neutral' },
      { text: 'Når du har trent nok, kan vi ta hovedmistenkten sammen.', mood: 'neutral' },
      { text: 'Da starter vi med litt sjekk så vi vet at du henger med.', mood: 'thinking' },
    ],
    next: 'quiz-mission',
  },

  'quiz-mission': {
    id: 'quiz-mission',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    lines: [{ text: 'Først: hva er hovedoppgaven din som nettdetektiv?', mood: 'thinking' }],
    choices: [
      { label: 'Å lage falske nyheter selv.', next: 'quiz-mission-wrong' },
      { label: 'Å avsløre løgner og falske bilder på nett.', next: 'features-intro' },
      { label: 'Å spille spill hele dagen og glemme leksene.', next: 'quiz-mission-wrong' },
    ],
  },
  'quiz-mission-wrong': {
    id: 'quiz-mission-wrong',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    lines: [
      { text: 'Hmm, det stemmer ikke. En nettdetektiv lager ikke løgner selv.', mood: 'thinking' },
      { text: 'Tenk igjen: jobben din er å se gjennom løgnene og finne sannheten.', mood: 'neutral' },
    ],
    next: 'quiz-mission',
  },

  'features-intro': {
    id: 'features-intro',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    lines: [
      { text: 'Riktig! Du skal være den som ser sannheten bak løgnene.', mood: 'happy' },
      { text: 'På detektivkontoret har du flere verktøy. La meg vise deg dem.', mood: 'neutral' },
    ],
    next: 'feature-map',
  },

  'feature-map': {
    id: 'feature-map',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_MAP,
    featureLabel: 'Kartet',
    lines: [
      { text: 'Først: Kartet. På kartet finner du detektivoppdrag på ulike steder i byen.', mood: 'neutral' },
      { text: 'Hvert sted har sin egen sak — én lærer deg om falske nyheter, en annen om manipulerte bilder.', mood: 'neutral' },
    ],
    next: 'quiz-map',
  },
  'quiz-map': {
    id: 'quiz-map',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_MAP,
    featureLabel: 'Kartet',
    lines: [{ text: 'Hva finner du på Kartet?', mood: 'thinking' }],
    choices: [
      { label: 'Detektivoppdrag på ulike steder i byen.', next: 'feature-mystery' },
      { label: 'Bare en pen oversikt over byen, uten oppdrag.', next: 'quiz-map-wrong' },
      { label: 'Telefonnummeret til hackeren.', next: 'quiz-map-wrong' },
    ],
  },
  'quiz-map-wrong': {
    id: 'quiz-map-wrong',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_MAP,
    featureLabel: 'Kartet',
    lines: [
      { text: 'Nesten! Husk hva jeg nettopp sa: kartet er fullt av oppdrag du kan løse.', mood: 'thinking' },
    ],
    next: 'quiz-map',
  },

  'feature-mystery': {
    id: 'feature-mystery',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_MYSTERY,
    featureLabel: 'Ukens Mysterium',
    lines: [
      { text: 'Bra! Neste verktøy: Ukens Mysterium.', mood: 'neutral' },
      { text: 'Hver uke får klassen din et nytt mysterium. Du leter etter spor på nettet og sender inn det du finner.', mood: 'neutral' },
    ],
    next: 'quiz-mystery',
  },
  'quiz-mystery': {
    id: 'quiz-mystery',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_MYSTERY,
    featureLabel: 'Ukens Mysterium',
    lines: [{ text: 'Hva er Ukens Mysterium?', mood: 'thinking' }],
    choices: [
      { label: 'En quiz læreren tar hver morgen.', next: 'quiz-mystery-wrong' },
      { label: 'En hemmelig hackerkode.', next: 'quiz-mystery-wrong' },
      { label: 'Et ukentlig oppdrag der jeg sender inn funn fra nettet.', next: 'feature-achievements' },
    ],
  },
  'quiz-mystery-wrong': {
    id: 'quiz-mystery-wrong',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_MYSTERY,
    featureLabel: 'Ukens Mysterium',
    lines: [
      { text: 'Ikke helt. Ukens Mysterium handler om at du selv leter og sender inn det du finner.', mood: 'thinking' },
    ],
    next: 'quiz-mystery',
  },

  'feature-achievements': {
    id: 'feature-achievements',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_ACHIEVEMENTS,
    featureLabel: 'Medaljer',
    lines: [
      { text: 'Akkurat. Så har vi Medaljer.', mood: 'neutral' },
      { text: 'Hver gang du fullfører et oppdrag eller gjør noe smart, får du en medalje du kan vise frem.', mood: 'happy' },
    ],
    next: 'quiz-achievements',
  },
  'quiz-achievements': {
    id: 'quiz-achievements',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_ACHIEVEMENTS,
    featureLabel: 'Medaljer',
    lines: [{ text: 'Hva er Medaljer for?', mood: 'thinking' }],
    choices: [
      { label: 'Noe man kjøper med ekte penger.', next: 'quiz-achievements-wrong' },
      { label: 'Belønninger jeg får når jeg fullfører oppdrag.', next: 'feature-notebook' },
      { label: 'De gir meg superkrefter i ekte liv.', next: 'quiz-achievements-wrong' },
    ],
  },
  'quiz-achievements-wrong': {
    id: 'quiz-achievements-wrong',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_ACHIEVEMENTS,
    featureLabel: 'Medaljer',
    lines: [
      { text: 'Nei, medaljer er noe du tjener selv ved å løse oppdrag. Prøv igjen.', mood: 'thinking' },
    ],
    next: 'quiz-achievements',
  },

  'feature-notebook': {
    id: 'feature-notebook',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_NOTEBOOK,
    featureLabel: 'Notatboka',
    lines: [
      { text: 'Bra jobba. Til slutt: Notatboka.', mood: 'neutral' },
      { text: 'I notatboka skriver du ned spor og funn fra oppdragene, så du husker dem til neste sak.', mood: 'neutral' },
      { text: 'Du har også Ledertavla, der du ser hvilken klasse på skolen som har flest poeng.', mood: 'happy' },
    ],
    next: 'quiz-notebook',
  },
  'quiz-notebook': {
    id: 'quiz-notebook',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_NOTEBOOK,
    featureLabel: 'Notatboka',
    lines: [{ text: 'Siste spørsmål: hva bruker du Notatboka til?', mood: 'thinking' }],
    choices: [
      { label: 'Å skrive ned spor og funn fra oppdragene.', next: 'detective-success' },
      { label: 'Å chatte med andre detektiver.', next: 'quiz-notebook-wrong' },
      { label: 'Å spille små minispill i pausen.', next: 'quiz-notebook-wrong' },
    ],
  },
  'quiz-notebook-wrong': {
    id: 'quiz-notebook-wrong',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    featureImage: FEATURE_NOTEBOOK,
    featureLabel: 'Notatboka',
    lines: [
      { text: 'Ikke helt. Notatboka er din egen logg over spor og funn.', mood: 'thinking' },
    ],
    next: 'quiz-notebook',
  },

  'detective-success': {
    id: 'detective-success',
    character: DETECTIVE,
    characterName: DETECTIVE_NAME,
    lines: [
      { text: 'Perfekt! Du har skjønt hvordan kontoret fungerer.', mood: 'happy' },
      { text: 'Da er du klar for ditt første ekte møte. Følg med — fienden er i nærheten.', mood: 'neutral' },
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
