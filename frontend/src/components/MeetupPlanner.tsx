"use client";

import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useProposeMeetup } from "@/lib/meetup-hooks";
import {
  useCrewBoard,
  useMyCrews,
  useRecommendations,
} from "@/lib/schedule-hooks";
import type { DayRecommendation } from "@/lib/types";
import {
  DAYS,
  formatTime,
  indexByDay,
  initialOf,
  keyOf,
  tintFor,
  type DayKey,
} from "@/lib/ui-helpers";

/** 다 같이 비는 시각부터 21시까지 정시 후보. 이미 지난 시간대는 자름. */
function timeOptions(availableFrom: string | null): string[] {
  const startHour =
    availableFrom === null
      ? 11
      : Math.min(Math.ceil(hourOf(availableFrom)), 21);
  const hours: string[] = [];
  for (let h = startHour; h <= 21; h += 1) {
    hours.push(`${String(h).padStart(2, "0")}:00`);
  }
  return hours;
}

function hourOf(time: string): number {
  const [h, m] = time.split(":").map(Number);
  return h + m / 60;
}

export function MeetupPlanner() {
  const crews = useMyCrews();
  const activeCrew = crews.data?.[0] ?? null;
  const board = useCrewBoard(activeCrew?.id ?? null);
  const recs = useRecommendations(activeCrew?.id ?? null);
  const proposeMutation = useProposeMeetup(activeCrew?.id ?? null);

  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [time, setTime] = useState<string | null>(null);
  const [title, setTitle] = useState("");

  if (crews.isLoading || board.isLoading || recs.isLoading) {
    return <p className="text-sm text-muted">불러오는 중이에요...</p>;
  }
  if (!activeCrew) {
    return (
      <p className="text-sm text-muted">
        크루가 있어야 약속을 잡을 수 있어요. 홈에서 먼저 크루를 만드세요.
      </p>
    );
  }
  if (!board.data || !recs.data) {
    return <p className="text-sm text-muted">데이터를 불러오지 못했어요.</p>;
  }

  const days: DayRecommendation[] = recs.data;
  const active = selectedDate
    ? (days.find((d) => d.date === selectedDate) ?? days[0])
    : days[0];
  const activeKey: DayKey = keyOf(active.dayOfWeek);
  const options = timeOptions(active.availableFrom);
  const bestRank = days.reduce((min, d) => Math.min(min, d.rank), Number.MAX_VALUE);

  const selectDate = (d: DayRecommendation) => {
    setSelectedDate(d.date);
    setTime(null);
    proposeMutation.reset();
  };

  const propose = () => {
    if (!time) return;
    proposeMutation.mutate({
      title: title.trim() || `${active.date} ${time} 모임`,
      meetDate: active.date,
      startTime: `${time}:00`,
      participantUserIds: [],
    });
  };

  const errorMessage =
    proposeMutation.error instanceof ApiError
      ? proposeMutation.error.message
      : proposeMutation.error
        ? "약속 제안에 실패했어요."
        : null;

  return (
    <div className="flex flex-col gap-5">
      {/* 1. 요일 선택 (추천 7일) */}
      <section className="rounded-[var(--radius-app)] border bg-surface p-5 shadow-sm">
        <h3 className="text-sm font-semibold tracking-tight">1. 언제 만날까요?</h3>
        <div className="mt-3 grid grid-cols-7 gap-1.5 sm:gap-2">
          {days.map((d) => {
            const label = DAYS.find((dd) => dd.iso === d.dayOfWeek)!.label;
            const selected = d.date === active.date;
            const rec = d.rank === bestRank;
            return (
              <button
                key={d.date}
                onClick={() => selectDate(d)}
                className={`flex flex-col items-center gap-0.5 rounded-xl border py-2.5 text-sm font-semibold transition-colors ${
                  selected
                    ? "border-primary bg-primary text-primary-foreground shadow-sm"
                    : "bg-surface hover:border-primary/40"
                }`}
              >
                <span>
                  {label}
                  {rec && (
                    <span className="ml-0.5" aria-label="추천">
                      ⭐
                    </span>
                  )}
                </span>
                <span
                  className={`text-[10px] font-normal ${
                    selected ? "text-primary-foreground/80" : "text-muted"
                  }`}
                >
                  {d.offCount}명 휴무
                </span>
              </button>
            );
          })}
        </div>
      </section>

      {/* 2. 멤버별 가능 시간 */}
      <section className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
        <div className="flex items-center justify-between gap-3 border-b px-5 py-3.5">
          <h3 className="text-sm font-semibold tracking-tight">
            2. {active.date} · 누가 언제부터 가능한가요?
          </h3>
          <span className="rounded-full bg-free-bg px-2.5 py-1 text-xs font-medium text-free">
            {active.allDayFree
              ? "하루 종일 모두 가능"
              : `${formatTime(active.availableFrom)} 이후 모두 가능`}
          </span>
        </div>

        <ul>
          {board.data.members.map((m, i) => {
            const slot = indexByDay(m.slots)[activeKey];
            return (
              <li
                key={m.userId}
                className={`flex items-center gap-3 px-5 py-3 ${
                  i > 0 ? "border-t" : ""
                }`}
              >
                <span
                  className={`grid size-8 shrink-0 place-items-center rounded-full text-xs font-semibold ${tintFor(m.userId)}`}
                >
                  {initialOf(m.nickname)}
                </span>
                <span className="w-14 truncate text-sm font-medium">{m.nickname}</span>

                {slot.off ? (
                  <span className="rounded-full bg-free-bg px-2.5 py-1 text-xs font-medium text-free">
                    휴무 · 하루 종일 가능
                  </span>
                ) : (
                  <span className="flex flex-wrap items-center gap-2 text-xs">
                    <span className="rounded-full bg-work-bg px-2.5 py-1 font-medium text-work">
                      근무 {formatTime(slot.startTime)}~{formatTime(slot.endTime)}
                    </span>
                    <span className="text-muted">
                      {formatTime(slot.endTime)} 이후 가능
                    </span>
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      </section>

      {/* 3. 시간 + 제목 + 제안 */}
      <section className="rounded-[var(--radius-app)] border bg-surface p-5 shadow-sm">
        <h3 className="text-sm font-semibold tracking-tight">
          3. 시간을 고르면 멤버들에게 제안해요
        </h3>

        <div className="mt-3 flex flex-wrap gap-2">
          {options.map((t) => {
            const selected = t === time;
            return (
              <button
                key={t}
                onClick={() => {
                  setTime(t);
                  proposeMutation.reset();
                }}
                className={`rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors ${
                  selected
                    ? "border-primary bg-primary text-primary-foreground shadow-sm"
                    : "bg-surface hover:border-primary/40"
                }`}
              >
                {t}
              </button>
            );
          })}
        </div>

        <label className="mt-4 flex flex-col gap-1.5">
          <span className="text-xs font-medium text-muted">약속 제목 (선택)</span>
          <input
            type="text"
            maxLength={100}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="rounded-lg border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
            placeholder="예: 목요일 저녁 모임"
          />
        </label>

        <div className="mt-5 flex flex-col gap-3 border-t pt-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm">
            {time ? (
              <>
                <span className="font-semibold">
                  {active.date} {time}
                </span>
                <span className="text-muted"> · 크루 전원에게 제안</span>
              </>
            ) : (
              <span className="text-muted">시간을 선택해 주세요.</span>
            )}
          </p>

          <button
            disabled={!time || proposeMutation.isPending}
            onClick={propose}
            className="inline-flex items-center justify-center gap-1.5 rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
          >
            {proposeMutation.isPending
              ? "제안 중..."
              : proposeMutation.isSuccess
                ? "제안 완료 ✓"
                : "이 시간으로 제안하기"}
          </button>
        </div>

        {errorMessage && (
          <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">
            {errorMessage}
          </p>
        )}
        {proposeMutation.isSuccess && (
          <p className="mt-3 rounded-xl bg-free-bg px-4 py-3 text-sm font-medium text-free">
            멤버들에게 약속 제안을 보냈어요! 응답이 오면 알려드릴게요.
          </p>
        )}
      </section>
    </div>
  );
}
