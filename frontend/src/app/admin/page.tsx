"use client";

import { useAdminStats } from "@/lib/admin-hooks";

export default function AdminDashboardPage() {
  const { data, isLoading } = useAdminStats();

  if (isLoading) return <p className="text-sm text-muted">불러오는 중...</p>;
  if (!data) return <p className="text-sm text-muted">통계를 불러오지 못했어요.</p>;

  const cards: { label: string; value: number; hint?: string; tone?: string }[] = [
    { label: "전체 유저", value: data.totalUsers },
    { label: "활성 유저", value: data.activeUsers, tone: "text-emerald-700" },
    {
      label: "정지 유저",
      value: data.suspendedUsers,
      tone: data.suspendedUsers > 0 ? "text-red-700" : undefined,
    },
    { label: "관리자", value: data.adminUsers, tone: "text-primary" },
    { label: "전체 크루", value: data.totalCrews },
    { label: "전체 약속", value: data.totalMeetups },
    {
      label: "미처리 신고",
      value: data.pendingReports,
      tone: data.pendingReports > 0 ? "text-red-700" : "text-muted",
      hint: "신고 탭에서 처리",
    },
  ];

  return (
    <section className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
      {cards.map((c) => (
        <div
          key={c.label}
          className="rounded-[var(--radius-app)] border bg-surface p-4 shadow-sm"
        >
          <p className="text-xs font-medium text-muted">{c.label}</p>
          <p className={`mt-1 text-2xl font-bold tabular-nums ${c.tone ?? ""}`}>{c.value}</p>
          {c.hint && <p className="mt-1 text-[10px] text-muted">{c.hint}</p>}
        </div>
      ))}
    </section>
  );
}
