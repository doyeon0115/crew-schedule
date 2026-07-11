"use client";

import { CommonFreeCard } from "@/components/CommonFreeCard";
import { CrewOnboarding } from "@/components/CrewOnboarding";
import { ScheduleGrid } from "@/components/ScheduleGrid";
import { useCrewBoard, useMyCrews, useRecommendations } from "@/lib/schedule-hooks";

export default function Home() {
  const crews = useMyCrews();
  const activeCrew = crews.data?.[0] ?? null;
  const board = useCrewBoard(activeCrew?.id ?? null);
  const recs = useRecommendations(activeCrew?.id ?? null);

  if (crews.isLoading) {
    return <p className="text-sm text-muted">불러오는 중...</p>;
  }
  if (!activeCrew) {
    return <CrewOnboarding />;
  }

  const topRec = recs.data?.find((r) => r.rank === 1) ?? null;
  const members = board.data ? board.data.members.map((m) => ({
    userId: m.userId,
    nickname: m.nickname,
    profileImageUrl: m.profileImageUrl,
    role: "MEMBER" as const,
  })) : [];

  return (
    <>
      <div className="mb-5">
        <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
          이번 주 · {activeCrew.name}
        </h1>
        <p className="mt-1 text-sm text-muted">
          다 같이 만날 수 있는 시간을 자동으로 찾아드려요.
        </p>
      </div>

      <div className="flex flex-col gap-5">
        <CommonFreeCard recommendation={topRec} members={members} />
        {board.data ? (
          <ScheduleGrid board={board.data} recommended={topRec} />
        ) : (
          <p className="rounded-[var(--radius-app)] border bg-surface p-5 text-sm text-muted">
            스케줄을 불러오는 중이에요.
          </p>
        )}
      </div>
    </>
  );
}
