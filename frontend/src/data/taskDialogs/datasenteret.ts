import type { DialogGraph } from '@/types/cutscene'
import { HACKER_IMAGE, HACKER_NAME } from '@/data/characters'

// The hacker uses a flat string `character` instead of a CharacterImages
// map — there's only one portrait for him, so the per-mood image swap
// in CutsceneDialog has nothing to choose from. Adding hacker moods to
// the dialog data here without first shipping the matching assets +
// HACKER_IMAGES map will silently render the same portrait for every line.
export const datasenteretOutro: DialogGraph = {
  passed: {
    id: 'passed',
    character: HACKER_IMAGE,
    characterName: HACKER_NAME,
    background: '/datasenteret.jpg',
    lines: [
      { text: 'Hva?! Det skulle ikke være mulig...', mood: 'angry' },
    ],
  },
  failed: {
    id: 'failed',
    character: HACKER_IMAGE,
    characterName: HACKER_NAME,
    background: '/datasenteret.jpg',
    lines: [
      { text: 'Hahaha! Ikke godt nok, detektiv.', mood: 'angry' },
    ],
  },
}
