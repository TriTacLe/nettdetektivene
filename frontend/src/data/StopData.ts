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
    background: '/NyhetskvartaletBackground.png',
    taskRoute: '/stops/nyhetskvartalet/task',
    taskHints: {
      1: {
        description: 'Ekte eller falsk nyhet?',
        hint: 'Les overskriften nøye og sjekk om kilden virker troverdig.',
      },
      2: {
        description: 'Identifiser kilden',
        hint: 'Hvem har skrevet artikkelen? Er nettsiden kjent og pålitelig?',
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
    intro: 'På dette stoppestedet undersøker vi bilder som kan være manipulerte eller skapt av KI. Tren øyet og finn ut hva som er ekte!',
    background: '/FotografenBackground.png',
    taskRoute: '/stops/fotografen/task',
    taskHints: {
      1: {
        description: 'Er bildet ekte?',
        hint: 'Se etter unaturlige detaljer som rare fingre, uskarp bakgrunn eller merkelig lys.',
      },
      2: {
        description: 'Finn manipulasjonen',
        hint: 'Legg merke til skygger, kanter og om elementer i bildet ser redigert ut.',
      },
      3: {
        description: 'KI eller ekte?',
        hint: 'KI-bilder har ofte feil på hender, refleksjoner og symmetri.',
      },
    },
  },
}
