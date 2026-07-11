"use client";

import {
  DAYS,
  formatTime,
  indexByDay,
  initialOf,
  keyOf,
  offCountForDay,
  tintFor,
  type DayKey,
} from "@/lib/ui-helpers";
import type { CrewScheduleBoard, DayRecommendation, SlotResponse } from "@/lib/types";

const GRID_COLS = "112px repeat(7, minmax(60px, 1fr))";

type Props = {
  board: CrewScheduleBoard;
  recommended: DayRecommendation | null;
};

export function ScheduleGrid({ board, recommended }: Props) {
  const highlightKey: DayKey | null = recommended ? keyOf(recommended.dayOfWeek) : null;

  return (
    <section className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
      <div className="flex items-center justify-between gap-3 border-b px-5 py-3.5">
        <h3 className="text-sm font-semibold tracking-tight">한눈에 보기</h3>
        <div className="flex items-center gap-3 text-xs text-muted">
          <Legend className="bg-free-bg text-free">쉬는 날</Legend>
          <Legend className="bg-work-bg text-work">근무</Legend>
        </div>
      </div>

      <div className="overflow-x-auto">
        <div className="min-w-[520px]">
          <div className="grid" style={{ gridTemplateColumns: GRID_COLS }}>
            <div className="px-4 py-2.5" />
            {DAYS.map((d) => {
              const rec = d.key === highlightKey;
              const off = offCountForDay(board.members, d.key);
              return (
                <div
                  key={d.key}
                  className={`flex flex-col items-center gap-0.5 py-2.5 text-center ${
                    rec ? "bg-amber-50/60" : ""
                  }`}
                >
                  <span
                    className={`text-sm font-semibold ${
                      rec ? "text-star" : "text-foreground"
                    }`}
                  >
                    {d.label}
                    {rec && <span className="ml-0.5">⭐</span>}
                  </span>
                  <span className="text-[10px] text-muted">{off}명 휴무</span>
                </div>
              );
            })}
          </div>

          {board.members.map((m, i) => {
            const week = indexByDay(m.slots);
            return (
              <div
                key={m.userId}
                className={`grid items-stretch ${i > 0 ? "border-t" : ""}`}
                style={{ gridTemplateColumns: GRID_COLS }}
              >
                <div className="flex items-center gap-2 px-4 py-2">
                  <span
                    className={`grid size-7 shrink-0 place-items-center rounded-full text-xs font-semibold ${tintFor(m.userId)}`}
                  >
                    {initialOf(m.nickname)}
                  </span>
                  <span className="truncate text-sm font-medium">{m.nickname}</span>
                </div>

                {DAYS.map((d) => (
                  <Cell
                    key={d.key}
                    slot={week[d.key]}
                    highlight={d.key === highlightKey}
                  />
                ))}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}

function Cell({ slot, highlight }: { slot: SlotResponse; highlight: boolean }) {
  return (
    <div
      className={`flex items-center justify-center p-1.5 ${
        highlight ? "bg-amber-50/40" : ""
      }`}
    >
      {slot.off ? (
        <div className="flex h-12 w-full items-center justify-center rounded-lg bg-free-bg text-xs font-medium text-free">
          휴무
        </div>
      ) : (
        <div className="flex h-12 w-full flex-col items-center justify-center rounded-lg bg-work-bg text-[11px] font-medium leading-tight text-work">
          <span>{formatTime(slot.startTime)}</span>
          <span className="text-work/50">~</span>
          <span>{formatTime(slot.endTime)}</span>
        </div>
      )}
    </div>
  );
}

function Legend({
  className,
  children,
}: {
  className: string;
  children: React.ReactNode;
}) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className={`size-3 rounded-[5px] ${className}`} />
      {children}
    </span>
  );
}
