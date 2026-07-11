"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuthStore } from "@/lib/auth-store";

const TABS = [
  { href: "/admin", label: "대시보드" },
  { href: "/admin/users", label: "유저" },
  { href: "/admin/reports", label: "신고" },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const user = useAuthStore((s) => s.user);
  const hydrated = useAuthStore((s) => s.hydrated);

  useEffect(() => {
    if (!hydrated) return;
    if (!user) {
      router.replace("/login");
    } else if (user.role !== "ADMIN") {
      router.replace("/");
    }
  }, [hydrated, user, router]);

  if (!hydrated || !user || user.role !== "ADMIN") {
    return <p className="text-sm text-muted">권한 확인 중...</p>;
  }

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h1 className="text-xl font-bold tracking-tight sm:text-2xl">관리자 콘솔</h1>
        <p className="mt-1 text-sm text-muted">유저와 크루, 부적절 컨텐츠를 관리해요.</p>
      </div>

      <nav className="flex gap-1 rounded-full border bg-surface p-1 self-start">
        {TABS.map((t) => {
          const active = pathname === t.href;
          return (
            <Link
              key={t.href}
              href={t.href}
              className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
                active
                  ? "bg-primary text-primary-foreground"
                  : "text-muted hover:text-foreground"
              }`}
            >
              {t.label}
            </Link>
          );
        })}
      </nav>

      {children}
    </div>
  );
}
