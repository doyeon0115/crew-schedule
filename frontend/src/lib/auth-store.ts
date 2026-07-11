"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { UserSummary } from "./types";

type AuthState = {
  user: UserSummary | null;
  accessToken: string | null;
  refreshToken: string | null;
  hydrated: boolean;
  setSession: (tokens: {
    user: UserSummary;
    accessToken: string;
    refreshToken: string;
  }) => void;
  setAccessToken: (token: string) => void;
  clear: () => void;
  markHydrated: () => void;
};

/** 브라우저 새로고침에도 세션 유지. localStorage에 accessToken/refreshToken/user 저장. */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      hydrated: false,
      setSession: ({ user, accessToken, refreshToken }) =>
        set({ user, accessToken, refreshToken }),
      setAccessToken: (accessToken) => set({ accessToken }),
      clear: () => set({ user: null, accessToken: null, refreshToken: null }),
      markHydrated: () => set({ hydrated: true }),
    }),
    {
      name: "crew-schedule-auth",
      partialize: (s) => ({
        user: s.user,
        accessToken: s.accessToken,
        refreshToken: s.refreshToken,
      }),
      onRehydrateStorage: () => (state) => {
        state?.markHydrated();
      },
    },
  ),
);
