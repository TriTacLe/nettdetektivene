import { http, HttpResponse } from 'msw'
import type { ApiResponse, StopTaskDTO, NewsChallenge, FotografenChallenge } from '@/types'

const BASE = import.meta.env.VITE_API_URL as string

const stopTasks: Record<string, StopTaskDTO[]> = {
  nyhetskvartalet: [
    { taskId: 1, title: 'Ekte eller falsk nyhet?', taskType: 'NEWS' },
    { taskId: 2, title: 'Identifiser kilden', taskType: 'NEWS' },
    { taskId: 3, title: 'Dobbeltsjekk nyheten', taskType: 'NEWS' },
  ],
  fotografen: [
    { taskId: 1, title: 'Er bildet ekte?', taskType: 'PHOTO' },
    { taskId: 2, title: 'Finn manipulasjonen', taskType: 'PHOTO' },
    { taskId: 3, title: 'KI eller ekte?', taskType: 'PHOTO' },
  ],
}

const newsChallenges: Record<number, NewsChallenge> = {
  1: {
    articles: [
      { imagePath: '/NyhetTest.png', isReal: false },
      { imagePath: '/NyhetTest.png', isReal: true },
    ],
  },
  2: {
    articles: [
      { imagePath: '/NyhetTest.png', isReal: true },
      { imagePath: '/NyhetTest.png', isReal: false },
    ],
  },
  3: {
    articles: [
      { imagePath: '/NyhetTest.png', isReal: false },
      { imagePath: '/NyhetTest.png', isReal: true },
    ],
  },
}

const fotografenChallenges: Record<number, FotografenChallenge> = {
  1: {
    alternatives: [
      {
        imagePath: '/FotografenTest.jpg',
        description: 'En person med uvanlig mange fingre og merkelig bakgrunn',
        correctType: 'ai-generert',
        begrunnelser: [
          'Fingrene ser unaturlige ut',
          'Bakgrunnen virker generert',
          'Lyset stemmer ikke',
          'Detaljene mangler konsistens',
        ],
      },
      {
        imagePath: '/FotografenTest.jpg',
        description: 'Skolebarn som leker i skolegård, lett bevegelsesuskarphet',
        correctType: 'ekte',
        begrunnelser: [
          'Naturlig bevegelsesuskarphet',
          'Realistiske skygger',
          'Konsistente detaljer',
          'Ingen tegn på redigering',
        ],
      },
    ],
  },
  2: {
    alternatives: [
      {
        imagePath: '/FotografenTest.jpg',
        description: 'Kjent politiker på ukjent sted – bakgrunnen virker lagt inn',
        correctType: 'manipulert',
        begrunnelser: [
          'Bakgrunnen ser innklipt ut',
          'Lyset stemmer ikke mellom person og bakgrunn',
          'Kantene virker redigerte',
          'Fargene er unaturlige',
        ],
      },
      {
        imagePath: '/FotografenTest.jpg',
        description: 'Dyr i et naturbilde – lyset ser unaturlig ut',
        correctType: 'ai-generert',
        begrunnelser: [
          'Lyset er urealistisk',
          'Pelsen mangler naturlig tekstur',
          'Bakgrunnen er for perfekt',
          'Detaljene ser genererte ut',
        ],
      },
    ],
  },
  3: {
    alternatives: [
      {
        imagePath: '/FotografenTest.jpg',
        description: 'Gammelt familiefoto, litt kornete og falmet',
        correctType: 'ekte',
        begrunnelser: [
          'Naturlig filmkorn',
          'Konsistent aldring av farger',
          'Ingen digitale artefakter',
          'Realistiske skygger',
        ],
      },
      {
        imagePath: '/FotografenTest.jpg',
        description: 'Bilde av en by med urealistisk perfekt belysning',
        correctType: 'ai-generert',
        begrunnelser: [
          'Lyset er for perfekt',
          'Kantene på bygningene er for skarpe',
          'Himmelen ser generert ut',
          'Ingen naturlige feil',
        ],
      },
    ],
  },
}

export const handlers = [
  http.get(`${BASE}/stops/:stopId/tasks`, ({ params }) => {
    const tasks = stopTasks[params.stopId as string]
    if (!tasks) {
      return HttpResponse.json<ApiResponse<null>>(
        { success: false, message: 'Stop not found', data: null },
        { status: 404 }
      )
    }
    return HttpResponse.json<ApiResponse<StopTaskDTO[]>>({
      success: true,
      message: 'OK',
      data: tasks,
    })
  }),

  http.get(`${BASE}/stops/:stopId/tasks/:taskId/challenge`, ({ params }) => {
    const stopId = params.stopId as string
    const taskId = Number(params.taskId)

    if (stopId === 'nyhetskvartalet') {
      const challenge = newsChallenges[taskId]
      if (!challenge) {
        return HttpResponse.json<ApiResponse<null>>(
          { success: false, message: 'Challenge not found', data: null },
          { status: 404 }
        )
      }
      return HttpResponse.json<ApiResponse<NewsChallenge>>({
        success: true,
        message: 'OK',
        data: challenge,
      })
    }

    if (stopId === 'fotografen') {
      const challenge = fotografenChallenges[taskId]
      if (!challenge) {
        return HttpResponse.json<ApiResponse<null>>(
          { success: false, message: 'Challenge not found', data: null },
          { status: 404 }
        )
      }
      return HttpResponse.json<ApiResponse<FotografenChallenge>>({
        success: true,
        message: 'OK',
        data: challenge,
      })
    }

    return HttpResponse.json<ApiResponse<null>>(
      { success: false, message: 'Stop not found', data: null },
      { status: 404 }
    )
  }),
]
