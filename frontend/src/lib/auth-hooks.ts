"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { apiRequest } from "./api";
import { useAuthStore } from "./auth-store";
import type { TokenResponse, UserSummary } from "./types";

type SignupInput = { email: string; password: string; nickname: string };
type LoginInput = { email: string; password: string };

export function useSignup() {
  const setSession = useAuthStore((s) => s.setSession);
  return useMutation({
    mutationFn: (input: SignupInput) =>
      apiRequest<TokenResponse>("/api/auth/signup", {
        method: "POST",
        body: input,
        auth: false,
      }),
    onSuccess: (data) => {
      setSession({
        user: data.user,
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      });
    },
  });
}

export function useLogin() {
  const setSession = useAuthStore((s) => s.setSession);
  return useMutation({
    mutationFn: (input: LoginInput) =>
      apiRequest<TokenResponse>("/api/auth/login", {
        method: "POST",
        body: input,
        auth: false,
      }),
    onSuccess: (data) => {
      setSession({
        user: data.user,
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      });
    },
  });
}

export function useLogout() {
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const clear = useAuthStore((s) => s.clear);
  return useMutation({
    mutationFn: async () => {
      if (refreshToken) {
        try {
          await apiRequest("/api/auth/logout", {
            method: "POST",
            body: { refreshToken },
            auth: false,
          });
        } catch {
          // 서버 실패해도 로컬 세션은 지운다
        }
      }
      clear();
    },
  });
}

/** 저장된 토큰이 유효한지 서버로 확인. 401이면 store.clear가 자동 발동. */
export function useMe(enabled: boolean) {
  return useQuery({
    queryKey: ["me"],
    queryFn: () => apiRequest<UserSummary>("/api/users/me"),
    enabled,
    staleTime: 60_000,
  });
}
