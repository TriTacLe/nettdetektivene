export interface AvatarConfig {
  skinColor: string
  hair: string
  hairColor: string
  hat: string
  clothing: string
  prop: string
}

export const SKIN_COLOR_OPTIONS = ['edb98a', 'd08b5b', '8d5524', '614335'] as const
export const HAIR_OPTIONS = ['frizzle', 'straight01', 'bigHair', 'dreads01'] as const
export const HAIR_COLOR_OPTIONS = ['2c1b18', '724133', 'b58143', 'c93305'] as const
export const HAT_OPTIONS = ['none', 'hat', 'hijab', 'winterHat03'] as const
export const CLOTHING_OPTIONS = ['hoodie', 'shirtCrewNeck', 'blazerAndShirt', 'collarAndSweater'] as const
export const PROP_OPTIONS = ['none', 'round', 'sunglasses', 'eyepatch'] as const

export const PROP_UNLOCK_LEVELS: Record<string, number> = {
  none: 0,
  round: 2,
  sunglasses: 3,
  eyepatch: 4,
}

export const DEFAULT_AVATAR_CONFIG: AvatarConfig = {
  skinColor: SKIN_COLOR_OPTIONS[0],
  hair: HAIR_OPTIONS[0],
  hairColor: HAIR_COLOR_OPTIONS[0],
  hat: HAT_OPTIONS[0],
  clothing: CLOTHING_OPTIONS[0],
  prop: 'none',
}
