export interface StopConfig {
  stopId: string
  name: string
  background: string
  taskRoute: string
}

export const STOPS: Record<string, StopConfig> = {
  nyhetskvartalet: {
    stopId: 'nyhetskvartalet',
    name: 'Nyhetskvartalet',
    background: '/NyhetskvartaletBackground.png',
    taskRoute: '/stops/nyhetskvartalet/task',
  },
  fotografen: {
    stopId: 'fotografen',
    name: 'Fotografen',
    background: '/FotografenBackground.png',
    taskRoute: '/stops/fotografen/task',
  },
}
