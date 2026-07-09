"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CREW } from "@/lib/mock";

const TABS = [
  { href: "/", label: "한눈에 보기" },
  { href: "/meetup", label: "약속 잡기" },
  { href: "/edit", label: "스케줄 수정" },
];

export function AppHeader() {
  const pathname = usePathname();

  return (
    <header className="sticky top-0 z-10 border-b bg-surface/80 backdrop-blur">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-5 py-3.5">
        <div className="flex items-center gap-2.5">
          <span className="grid size-9 place-items-center rounded-[var(--radius-app)] bg-primary text-primary-foreground shadow-sm">
            <CalendarIcon />
          </span>
          <div className="leading-tight">
            <p className="text-sm font-semibold tracking-tight">Crew Schedule</p>
            <p className="text-xs text-muted">{CREW.name}</p>
          </div>
        </div>

        <div className="flex items-center gap-2 rounded-full border bg-surface px-3 py-1.5">
          <span className="relative flex size-2">
            <span className="absolute inline-flex size-full animate-ping rounded-full bg-emerald-400 opacity-75" />
            <span className="relative inline-flex size-2 rounded-full bg-emerald-500" />
          </span>
          <span className="text-xs font-medium text-muted">
            {CREW.online}명 접속
          </span>
        </div>
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
