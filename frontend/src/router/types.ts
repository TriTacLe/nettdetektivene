// Augments vue-router's RouteMeta so route definitions get type-checked
// for the game-feel meta fields (music, background, vignette, scanlines).
import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    music?: 'dark' | 'main' | string
    background?: string
    vignette?: boolean
    scanlines?: boolean
  }
}
