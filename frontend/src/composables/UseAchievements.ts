import { useQuery } from '@tanstack/vue-query'
import { getAllAchievementsOptions } from '@/generated-client/@tanstack/vue-query.gen'

export function useAchievements() {
  return useQuery(getAllAchievementsOptions())
}
