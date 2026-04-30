import { ref, computed } from 'vue'

let createAvatarFn: ((_: any) => any) | null = null
let avataaarsStyle: any = null

export async function loadDicebear() {
  if (createAvatarFn && avataaarsStyle) {
    return { createAvatar: createAvatarFn, avataaars: avataaarsStyle }
  }
  
  const [{ createAvatar }, { avataaars }] = await Promise.all([
    import('@dicebear/core'),
    import('@dicebear/collection')
  ])
  
  createAvatarFn = createAvatar
  avataaarsStyle = avataaars
  
  return { createAvatar, avataaars }
}

export function useAvatarLoader() {
  const isLoading = ref(false)
  const isLoaded = ref(createAvatarFn !== null)
  
  const load = async () => {
    if (isLoaded.value) return
    isLoading.value = true
    
    try {
      await loadDicebear()
      isLoaded.value = true
    } catch (error) {
      console.error('Failed to load avatar generator:', error)
    } finally {
      isLoading.value = false
    }
  }
  
  return {
    isLoading: computed(() => isLoading.value),
    isLoaded: computed(() => isLoaded.value),
    load
  }
}