import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User, UserRole } from '../types';

export interface AuthStoreState {
  currentUser: User | null;
  token: string | null;
  setCurrentUser: (user: User | null) => void;
  setToken: (token: string | null) => void;
  isAuthenticated: () => boolean;
  hasRole: (role: UserRole) => boolean;
  logout: () => void;
}

export const useAuthStore = create<AuthStoreState>()(
  persist(
    (set, get) => ({
      currentUser: null,
      token: null,
      setCurrentUser: (user: User | null): void => set({ currentUser: user }),
      setToken: (token: string | null): void => set({ token }),
      isAuthenticated: (): boolean => {
        const state = get();
        return state.currentUser !== null && state.token !== null;
      },
      hasRole: (role: UserRole): boolean => {
        const state = get();
        return state.currentUser?.role === role;
      },
      logout: (): void => {
        set({ currentUser: null, token: null });
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);


