export function useFocusTrap(dialogId: string, onEscape: () => void) {
  function onKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') {
      onEscape()
      return
    }
    if (e.key !== 'Tab') return
    e.preventDefault()
    const dialog = document.getElementById(dialogId)
    const focusable = Array.from(
      dialog?.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled])') ?? [],
    )
    if (focusable.length === 0) return
    const currentIdx = focusable.indexOf(document.activeElement as HTMLElement)
    if (e.shiftKey) {
      focusable[currentIdx <= 0 ? focusable.length - 1 : currentIdx - 1].focus()
    } else {
      focusable[currentIdx >= focusable.length - 1 ? 0 : currentIdx + 1].focus()
    }
  }

  return { onKeydown }
}
