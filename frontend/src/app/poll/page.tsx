"use client";

import { useState } from "react";
import { PollCard } from "@/components/PollCard";
import { PollCreator } from "@/components/PollCreator";
import { useCrewPolls } from "@/lib/poll-hooks";
import { useMyCrews } from "@/lib/schedule-hooks";

export default function PollPage() {
  const crews = useMyCrews();
  const activeCrew = crews.data?.[0] ?? null;
  const polls = useCrewPolls(activeCrew?.id ?? null);
  const [showCreator, setShowCreator] = useState(false);

  if (crews.isLoading) return <p className="text-sm text-muted">불러오는 중...</p>;
  if (!activeCrew) {
    return (
      <p className="text-sm text-muted">
        크루가 있어야 투표를 만들 수 있어요. 홈에서 먼저 크루를 만드세요.
      </p>
    );
  }

  return (
    <>
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
            날짜 투표 · {activeCrew.name}
          </h1>
          <p className="mt-1 text-sm text-muted">
            후보 날짜를 여러 개 열어두고 다 함께 투표해요.
          </p>
        </div>
        <button
          onClick={() => setShowCreator((v) => !v)}
          className="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm hover:scale-[1.02] active:scale-95"
        >
          {showCreator ? "닫기" : "새 투표"}
        </button>
      </div>

      <div className="flex flex-col gap-5">
        {showCreator && <PollCreator crewId={activeCrew.id} onCreated={() => setShowCreator(false)} />}

        {polls.isLoading ? (
          <p className="text-sm text-muted">투표를 불러오는 중...</p>
        ) : !polls.data || polls.data.length === 0 ? (
          <div className="rounded-[var(--radius-app)] border bg-surface p-8 text-center text-sm text-muted">
            아직 만들어진 투표가 없어요.
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            {polls.data.map((p) => (
              <PollCard key={p.id} poll={p} />
            ))}
          </div>
        )}
      </div>
    </>
  );
}
