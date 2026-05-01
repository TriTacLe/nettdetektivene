import type { DialogScene } from '@/types/cutscene'
import nyhetskvartaletPicker from './nyhetskvartaletPicker'
import fotografenPicker from './fotografenPicker'
import datasenteretPicker from './datasenteretPicker'

export const STOP_PICKERS: Record<string, DialogScene> = {
  nyhetskvartalet: nyhetskvartaletPicker,
  fotografen: fotografenPicker,
  datasenteret: datasenteretPicker,
}
