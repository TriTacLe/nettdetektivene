import type { DialogGraph } from '@/types/cutscene'
import { nyhetskvartaletOutro } from './nyhetskvartalet'
import { fotografenOutro } from './fotografen'
import { datasenteretOutro } from './datasenteret'

export const STOP_OUTROS: Record<string, DialogGraph> = {
  nyhetskvartalet: nyhetskvartaletOutro,
  fotografen: fotografenOutro,
  datasenteret: datasenteretOutro,
}
