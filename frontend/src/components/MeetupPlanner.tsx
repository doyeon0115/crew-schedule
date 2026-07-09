"use client";

import { useState } from "react";
import {
  commonFreeFrom,
  DAYS,
  MEMBERS,
  offCount,
  RECOMMENDED,
  type DayKey,
} from "@/lib/mock";

/** 다 같이 비는 시각부터 21시까지 정시 후보 (전원 휴무면 낮 시간대 포함) */
function timeOptions(dayKey: DayKey): string[] {
  const from = commonFreeFrom(dayKey);
  const startHour = from === null ? 11 : Math.min(Math.ceil(hour(from)), 21);
  const hours: string[] = [];
  for (let h = startHour; h <= 21; h += 1) {
    hours.push(`${String(h).padStart(2, "0")}:00`);
  }
  return hours;
}

function hour(time: string): number {
  const [h, m] = time.split(":").map(Number);
  return h + m / 60;
}

export function MeetupPlanner() {
  const [dayKey, setDayKey] = useState<DayKey>(RECOMMENDED.dayKey);
  const [time, setTime] = useState<string | null>(null);
  const [proposed, setProposed] = useState(false);

  const dayLabel = DAYS.find((d) => d.key === dayKey)!.label;
  const freeFrom = commonFreeFrom(dayKey);
  const options = timeOptions(dayKey);

  const selectDay = (key: DayKey) => {
    setDayKey(key);
    setTime(null);
    setProposed(false);
  };

  return (
    <div className="flex flex-col gap-5">
      {/* 1. 요일 선택 */}
      <section className="rounded-[var(--radius-app)] border bg-surface p-5 shadow-sm">
        <h3 className="text-sm font-semibold tracking-tight">
          1. 언제 만날까요?
        </h3>
        <div className="mt-3 grid grid-cols-7 gap-1.5 sm:gap-2">
          {DAYS.map((d) => {
            const selected = d.key === dayKey;
            const rec = d.key === RECOMMENDED.dayKey;
            return (
              <button
                key={d.key}
                onClick={() => selectDay(d.key)}
                className={`flex flex-col items-center gap-0.5 rounded-xl border py-2.5 text-sm font-semibold transition-colors ${
                  selected
                    ? "border-primary bg-primary text-primary-foreground shadow-sm"
                    : "bg-surface hover:border-primary/40"
                }`}
              >
                <span>
                  {d.label}
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
                  {offCount(d.key)}명 휴무
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
            2. {dayLabel}요일, 누가 언제부터 가능한가요?
          </h3>
          <span className="rounded-full bg-free-bg px-2.5 py-1 text-xs font-medium text-free">
            {freeFrom === null ? "하루 종일 모두 가능" : `${freeFrom} 이후 모두 가능`}
          </span>
        </div>

        <ul>
          {MEMBERS.map((m, i) => {
            const slot = m.sched[dayKey];
            return (
              <li
                key={m.id}
                className={`flex items-center gap-3 px-5 py-3 ${
                  i > 0 ? "border-t" : ""
                }`}
              >
                <span
                  className={`grid size-8 shrink-0 place-items-center rounded-full text-xs font-semibold ${m.tint}`}
                >
                  {m.initial}
                </span>
                <span className="w-14 truncate text-sm font-medium">
                  {m.name}
                </span>

                {slot.off ? (
                  <span className="rounded-full bg-free-bg px-2.5 py-1 text-xs font-medium text-free">
                    휴무 · 하루 종일 가능
                  </span>
                ) : (
                  <span className="flex flex-wrap items-center gap-2 text-xs">
                    <span className="rounded-full bg-work-bg px-2.5 py-1 font-medium text-work">
                      근무 {slot.start}~{slot.end}
                    </span>
                    <span className="text-muted">{slot.end} 이후 가능</span>
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      </section>

      {/* 3. 시간 선택 + 제안 */}
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
                  setProposed(false);
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

        <div className="mt-5 flex flex-col gap-3 border-t pt-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm">
            {time ? (
              <>
                <span className="font-semibold">
                  {dayLabel}요일 {time}
                </span>
                <span className="text-muted"> · {MEMBERS.length}명 모두 가능</span>
              </>
            ) : (
              <span className="text-muted">시간을 선택해 주세요.</span>
            )}
          </p>

          <button
            disabled={!time || proposed}
            onClick={() => setProposed(true)}
            className="inline-flex items-center justify-center gap-1.5 rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
          >
            {proposed ? "제안 완료 ✓" : "이 시간으로 제안하기"}
          </button>
        </div>

        {proposed && (
          <p className="mt-3 rounded-xl bg-free-bg px-4 py-3 text-sm font-medium text-free">
            멤버들에게 약속 제안을 보냈어요! 응답이 오면 알려드릴게요. (목업 —
            백엔드 연결 전)
          </p>
        )}
      </section>
    </div>
  );
}
