"use client";

import { ApiError } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { useClosePoll, useUnvote, useVote } from "@/lib/poll-hooks";
import type { Poll, PollCandidate } from "@/lib/types";
import { formatTime } from "@/lib/ui-helpers";

type Props = { poll: Poll };

export function PollCard({ poll }: Props) {
  const me = useAuthStore((s) => s.user);
  const vote = useVote(poll.id, poll.crewId);
  const unvote = useUnvote(poll.id, poll.crewId);
  const close = useClosePoll(poll.id, poll.crewId);

  const isClosed = poll.status === "CLOSED";
  const isCreator = me?.id === poll.creatorId;
  const totalVotes = poll.candidates.reduce((sum, c) => sum + c.voteCount, 0);
  const errorMessage = (() => {
    for (const m of [vote, unvote, close]) {
      if (m.error instanceof ApiError) return m.error.message;
    }
    return null;
  })();

  const toggle = (c: PollCandidate) => {
    if (isClosed) return;
    if (c.votedByMe) unvote.mutate(c.id);
    else vote.mutate(c.id);
  };

  return (
    <article className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
      <div className="flex items-center justify-between gap-3 border-b px-5 py-3">
        <div className="min-w-0">
          <h3 className="truncate text-base font-semibold tracking-tight">{poll.title}</h3>
          <p className="text-xs text-muted">
            총 {totalVotes}표 · {poll.candidates.length}개 후보
          </p>
        </div>
        <span
          className={`rounded-full px-2 py-0.5 text-[11px] font-bold ${
            isClosed
              ? "bg-neutral-100 text-muted"
              : "bg-primary/10 text-primary"
          }`}
        >
          {isClosed ? "마감" : "진행 중"}
        </span>
      </div>

      <ul>
        {poll.candidates.map((c, i) => {
          const isWinner = poll.winnerCandidateId === c.id;
          const pct = totalVotes === 0 ? 0 : Math.round((c.voteCount / totalVotes) * 100);
          return (
            <li
              key={c.id}
              className={`relative flex items-center gap-3 px-5 py-3 ${
                i > 0 ? "border-t" : ""
              } ${isWinner ? "bg-amber-50/60" : ""}`}
            >
              {/* 배경 gauge */}
              <span
                className="absolute inset-y-0 left-0 bg-primary/5"
                style={{ width: `${pct}%` }}
                aria-hidden
              />
              <button
                onClick={() => toggle(c)}
                disabled={isClosed || vote.isPending || unvote.isPending}
                className={`relative shrink-0 rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                  c.votedByMe
                    ? "border-primary bg-primary text-primary-foreground"
                    : "bg-surface hover:border-primary/40"
                } disabled:cursor-not-allowed disabled:opacity-50`}
              >
                {c.votedByMe ? "투표됨" : "투표"}
              </button>
              <div className="relative flex-1 min-w-0">
                <p className="text-sm font-medium">
                  {c.date}
                  {c.startTime && ` · ${formatTime(c.startTime)}`}
                  {isWinner && (
                    <span className="ml-2 rounded-full bg-amber-200 px-1.5 py-0.5 text-[10px] font-bold text-amber-900">
                      ⭐ 확정
                    </span>
                  )}
                </p>
              </div>
              <span className="relative text-xs font-bold tabular-nums">
                {c.voteCount}표
              </span>
            </li>
          );
        })}
      </ul>

      {errorMessage && (
        <p className="border-t bg-red-50 px-4 py-2 text-xs text-red-700">{errorMessage}</p>
      )}

      {!isClosed && isCreator && (
        <div className="flex items-center justify-end border-t bg-background/50 px-4 py-3">
          <button
            onClick={() => close.mutate()}
            disabled={close.isPending || totalVotes === 0}
            className="rounded-full bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground shadow-sm hover:scale-[1.02] active:scale-95 disabled:opacity-40"
          >
            투표 마감하기
          </button>
        </div>
      )}
    </article>
  );
}
