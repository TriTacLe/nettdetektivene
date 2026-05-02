export interface TaskHint {
  description: string
  hint: string
}

export interface StopConfig {
  stopId: string
  name: string
  intro: string
  background: string
  taskRoute: string
  taskHints?: Record<number, TaskHint>
}

export const STOPS: Record<string, StopConfig> = {
  nyhetskvartalet: {
    stopId: 'nyhetskvartalet',
    name: 'Nyhetskvartalet',
    intro: 'Her jobber nettdetektivene med å avsløre falske nyheter. Klarer du å skille ekte journalistikk fra løgn og manipulasjon?',
    background: '/NyhetskvartaletBackground.jpg',
    taskRoute: '/stops/nyhetskvartalet',
    taskHints: {
      1: {
        description: 'Se på overskriften',
        hint: 'Les overskriften nøye og sjekk om bildene ser troverdige ut. Er det noen åpenbare feil eller overdrivelser?',
      },
      2: {
        description: 'Se på bildene',
        hint: 'Ser bildene ekte ut? Er det noe rart med bildene?',
      },
      3: {
        description: 'Dobbeltsjekk nyheten',
        hint: 'Kan du finne den samme nyheten på andre troverdige nettsteder?',
      },
    },
  },
  fotografen: {
    stopId: 'fotografen',
    name: 'Fotografen',
    intro: 'På dette stoppestedet undersøker vi bilder som kan være manipulerte eller skapt av Kunstig Intelligens. Tren øyet og finn ut hva som er ekte!',
    background: '/FotografenBackground.png',
    taskRoute: '/stops/fotografen',
  },
  datasenteret: {
    stopId: 'datasenteret',
    name: 'Datasenteret',
    intro: 'Velkommen til hjertet av dataverdenen. Her møter du datatyven sin siste prøvelse — en blanding av alle utfordringene du har lært deg å løse.',
    background: '/datasenteret.jpg',
    taskRoute: '/stops/datasenteret',
  },
}
