"use client";

import { useAuthStore } from "./auth-store";
import type { ApiResponse, TokenResponse } from "./types";

/**
 * 백엔드 API 호출 표준 클라이언트.
 *
 * - Bearer 토큰 자동 첨부
 * - 401 발생 시 refresh 토큰으로 access 갱신 후 한 번만 재시도
 * - {@link ApiResponse} 껍질을 벗겨 data만 반환. 실패 시 message로 throw.
 */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

type Options = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  auth?: boolean; // 기본 true
};

export async function apiRequest<T>(path: string, opts: Options = {}): Promise<T> {
  const { method = "GET", body, auth = true } = opts;
  const res = await doFetch(path, method, body, auth);
  if (res.status === 401 && auth && (await tryRefresh())) {
    return handleResponse<T>(await doFetch(path, method, body, auth));
  }
  return handleResponse<T>(res);
}

async function doFetch(path: string, method: string, body: unknown, auth: boolean) {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (auth) {
    const token = useAuthStore.getState().accessToken;
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }
  return fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    cache: "no-store",
  });
}

async function handleResponse<T>(res: Response): Promise<T> {
  const text = await res.text();
  let parsed: ApiResponse<T> | undefined;
  try {
    parsed = text ? (JSON.parse(text) as ApiResponse<T>) : undefined;
  } catch {
    // 백엔드는 항상 ApiResponse를 반환하므로 여기 들어오면 프록시/네트워크 문제.
  }
  if (!res.ok) {
    const message = parsed?.message ?? `HTTP ${res.status}`;
    const code = parsed?.code ?? "HTTP_ERROR";
    throw new ApiError(res.status, code, message);
  }
  if (!parsed) {
    throw new ApiError(res.status, "PARSE_ERROR", "응답을 해석할 수 없습니다.");
  }
  return parsed.data;
}

/** 토큰 재발급. 성공하면 store에 새 accessToken 반영하고 true. */
async function tryRefresh(): Promise<boolean> {
  const store = useAuthStore.getState();
  const refresh = store.refreshToken;
  if (!refresh) return false;
  try {
    const res = await fetch("/api/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: refresh }),
      cache: "no-store",
    });
    if (!res.ok) {
      store.clear();
      return false;
    }
    const parsed = (await res.json()) as ApiResponse<TokenResponse>;
    store.setSession({
      user: parsed.data.user,
      accessToken: parsed.data.accessToken,
      refreshToken: parsed.data.refreshToken,
    });
    return true;
  } catch {
    store.clear();
    return false;
  }
}
