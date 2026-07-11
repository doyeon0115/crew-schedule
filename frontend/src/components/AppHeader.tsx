"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useLogout } from "@/lib/auth-hooks";
import { useAuthStore } from "@/lib/auth-store";
import { useMyCrews } from "@/lib/schedule-hooks";

const TABS = [
  { href: "/", label: "한눈에 보기" },
  { href: "/meetup", label: "약속 잡기" },
  { href: "/chat", label: "채팅" },
  { href: "/edit", label: "스케줄 수정" },
];

export function AppHeader() {
  const pathname = usePathname();
  const user = useAuthStore((s) => s.user);
  const crews = useMyCrews();
  const crewName = crews.data?.[0]?.name;
  const logout = useLogout();

  return (
    <header className="sticky top-0 z-10 border-b bg-surface/80 backdrop-blur">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-5 py-3.5">
        <div className="flex items-center gap-2.5">
          <span className="grid size-9 place-items-center rounded-[var(--radius-app)] bg-primary text-primary-foreground shadow-sm">
            <CalendarIcon />
          </span>
          <div className="leading-tight">
            <p className="text-sm font-semibold tracking-tight">Crew Schedule</p>
            <p className="text-xs text-muted">{crewName ?? "크루 없음"}</p>
          </div>
        </div>

        {user && (
          <div className="flex items-center gap-3">
            <span className="text-xs font-medium text-muted">{user.nickname}</span>
            <button
              onClick={() => logout.mutate()}
              className="rounded-full border px-3 py-1.5 text-xs font-medium text-muted transition-colors hover:border-primary/40 hover:text-foreground"
            >
              로그아웃
            </button>
          </div>
        )}
      </div>

      <nav className="mx-auto flex max-w-5xl gap-1 px-4">
        {TABS.map((t) => {
          const active = pathname === t.href;
          return (
            <Link
              key={t.href}
              href={t.href}
              className={`relative px-3.5 py-2.5 text-sm font-medium transition-colors ${
                active ? "text-primary" : "text-muted hover:text-foreground"
              }`}
            >
              {t.label}
              {active && (
                <span className="absolute inset-x-3 -bottom-px h-0.5 rounded-full bg-primary" />
              )}
            </Link>
          );
        })}
      </nav>
    </header>
  );
}

function CalendarIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" />
    </svg>
  );
}
