import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  token: string | null
  refreshToken: string | null
  user: { email: string; fullName: string; role: string; onboardingCompleted?: boolean } | null
  setAuth: (token: string, refreshToken: string, user: AuthState['user']) => void
  setToken: (token: string) => void
  setOnboardingCompleted: (v: boolean) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      user: null,
      setAuth: (token, refreshToken, user) => set({ token, refreshToken, user }),
      setToken: (token) => set({ token }),
      setOnboardingCompleted: (v) =>
        set((s) => ({ user: s.user ? { ...s.user, onboardingCompleted: v } : s.user })),
      clearAuth: () => set({ token: null, refreshToken: null, user: null }),
    }),
    { name: 'docint-auth' }
  )
)
