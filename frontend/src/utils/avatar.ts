import {
  DEFAULT_AVATAR_CONFIG,
  SKIN_COLOR_OPTIONS,
  HAIR_OPTIONS,
  HAIR_COLOR_OPTIONS,
  HAT_OPTIONS,
  CLOTHING_OPTIONS,
  PROP_OPTIONS,
  type AvatarConfig,
} from '@/types/avatar'

const VALID_SKIN_COLORS = new Set<string>(SKIN_COLOR_OPTIONS)
const VALID_HAIRS = new Set<string>(HAIR_OPTIONS)
const VALID_HAIR_COLORS = new Set<string>(HAIR_COLOR_OPTIONS)
const VALID_HATS = new Set<string>(HAT_OPTIONS)
const VALID_CLOTHING = new Set<string>(CLOTHING_OPTIONS)
const VALID_PROPS = new Set<string>(PROP_OPTIONS)

export function parseAvatarConfig(json: string | null): AvatarConfig {
  if (!json) return { ...DEFAULT_AVATAR_CONFIG }
  try {
    const p = JSON.parse(json)
    if (typeof p !== 'object' || p === null) return { ...DEFAULT_AVATAR_CONFIG }

    return {
      skinColor: VALID_SKIN_COLORS.has(p.skinColor) ? p.skinColor : DEFAULT_AVATAR_CONFIG.skinColor,
      hair: VALID_HAIRS.has(p.hair) ? p.hair : DEFAULT_AVATAR_CONFIG.hair,
      hairColor: VALID_HAIR_COLORS.has(p.hairColor) ? p.hairColor : DEFAULT_AVATAR_CONFIG.hairColor,
      hat: VALID_HATS.has(p.hat) ? p.hat : DEFAULT_AVATAR_CONFIG.hat,
      clothing: VALID_CLOTHING.has(p.clothing) ? p.clothing : DEFAULT_AVATAR_CONFIG.clothing,
      prop: (typeof p.prop === 'string' && VALID_PROPS.has(p.prop)) ? p.prop : 'none',
    }
  } catch {
    return { ...DEFAULT_AVATAR_CONFIG }
  }
}

export function serializeAvatarConfig(cfg: AvatarConfig): string {
  return JSON.stringify(cfg)
}
