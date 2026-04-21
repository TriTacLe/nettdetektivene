import type { NavigationGuardNext, RouteLocationNormalized } from "vue-router";
import { useAuthStore } from "@/stores/AuthStore.ts";

export function requireAuth(
  _to: RouteLocationNormalized,
  _from: RouteLocationNormalized,
  next: NavigationGuardNext
): void {
  const auth = useAuthStore()
  if (!auth.isAuthenticated) {
    next({ name: 'start' })
  } else {
    next()
  }
}

export function requireTeacher(
  _to: RouteLocationNormalized,
  _from: RouteLocationNormalized,
  next: NavigationGuardNext,
): void {
  const auth = useAuthStore()
  if (!auth.isAuthenticated || !auth.isTeacher) {
    next({ name: 'start' })
  } else {
    next()
  }
}

export function requireStudent(
  _to: RouteLocationNormalized,
  _from: RouteLocationNormalized,
  next: NavigationGuardNext,
): void {
  const auth = useAuthStore()
  if (!auth.isAuthenticated || !auth.isStudent) {
    next({ name: 'start' })
  } else {
    next()
  }
}
