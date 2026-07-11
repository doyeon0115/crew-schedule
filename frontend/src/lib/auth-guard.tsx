"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuthStore } from "./auth-store";

const PUBLIC_ROUTES = new Set(["/login", "/signup"]);

/**
 * 인증되지 않은 유저를 /login으로 리다이렉트, 로그인 상태에서 /login·/signup 접근 시 홈으로 보냄.
 * hydration이 끝나기 전에는 아무 동작도 하지 않아 SSR flicker 방지.
 */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const hydrated = useAuthStore((s) => s.hydrated);
  const accessToken = useAuthStore((s) => s.accessToken);

  useEffect(() => {
    if (!hydrated) return;
    const isPublic = PUBLIC_ROUTES.has(pathname);
    if (!accessToken && !isPublic) {
      router.replace("/login");
    } else if (accessToken && isPublic) {
      router.replace("/");
    }
  }, [hydrated, accessToken, pathname, router]);

  return children;
}
