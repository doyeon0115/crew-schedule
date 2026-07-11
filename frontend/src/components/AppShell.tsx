"use client";

import { usePathname } from "next/navigation";
import { AppHeader } from "./AppHeader";

const HEADERLESS_ROUTES = new Set(["/login", "/signup"]);

/** 로그인/회원가입 화면에서는 헤더/푸터를 감추고 폼만 중앙에 보이도록 감싸는 셸. */
export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const headerless = HEADERLESS_ROUTES.has(pathname);

  if (headerless) {
    return (
      <div className="mx-auto w-full max-w-5xl flex-1 px-5">{children}</div>
    );
  }

  return (
    <div className="flex flex-1 flex-col">
      <AppHeader />
      <main className="mx-auto w-full max-w-5xl flex-1 px-5 py-6">
        {children}
      </main>
      <footer className="mx-auto w-full max-w-5xl px-5 py-6 text-center text-xs text-muted">
        Crew Schedule · 우리끼리 스케줄
      </footer>
    </div>
  );
}
