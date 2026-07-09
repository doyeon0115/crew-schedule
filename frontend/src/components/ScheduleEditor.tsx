"use client";

import { useState } from "react";
import { DAYS, MEMBERS, type DayKey, type Slot } from "@/lib/mock";

type WeekSched = Record<DayKey, Slot>;

function cloneSched(sched: WeekSched): WeekSched {
  return Object.fromEntries(
    Object.entries(sched).map(([k, s]) => [k, { ...s }]),
  ) as WeekSched;
}

export function ScheduleEditor() {
  const [memberId, setMemberId] = useState(MEMBERS[0].id);
  const [drafts, setDrafts] = useState<Record<string, WeekSched>>(() =>
    Object.fromEntries(MEMBERS.map((m) => [m.id, cloneSched(m.sched)])),
  );
  const [saved, setSaved] = useState(false);

  const member = MEMBERS.find((m) => m.id === memberId)!;
  const draft = drafts[memberId];

  const update = (dayKey: DayKey, patch: Partial<Slot>) => {
    setDrafts((prev) => ({
      ...prev,
      [memberId]: {
        ...prev[memberId],
        [dayKey]: { ...prev[memberId][dayKey], ...patch },
      },
    }));
    setSaved(false);
  };

  return (
    <div className="flex flex-col gap-5">
      {/* 멤버 선택 */}
      <section className="rounded-[var(--radius-app)] border bg-surface p-5 shadow-sm">
        <h3 className="text-sm font-semibold tracking-tight">
          누구의 스케줄인가요?
        </h3>
        <div className="mt-3 flex flex-wrap gap-2">
          {MEMBERS.map((m) => {
            const selected = m.id === memberId;
            return (
              <button
                key={m.id}
                onClick={() => {
                  setMemberId(m.id);
                  setSaved(false);
                }}
                className={`flex items-center gap-2 rounded-full border py-1.5 pl-1.5 pr-4 text-sm font-medium transition-colors ${
                  selected
                    ? "border-primary bg-primary/5 text-primary"
                    : "bg-surface hover:border-primary/40"
                }`}
              >
                <span
                  className={`grid size-7 place-items-center rounded-full text-xs font-semibold ${m.tint}`}
                >
                  {m.initial}
                </span>
                {m.name}
              </button>
            );
          })}
        </div>
      </section>

      {/* 주간 스케줄 편집 */}
      <section className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
        <div className="flex items-center justify-between gap-3 border-b px-5 py-3.5">
          <h3 className="text-sm font-semibold tracking-tight">
            {member.name}의 이번 주 근무
          </h3>
          <span className="text-xs text-muted">
            휴무를 끄면 근무 시간을 입력할 수 있어요
          </span>
        </div>

        <ul>
          {DAYS.map((d, i) => {
            const slot = draft[d.key];
            return (
              <li
                key={d.key}
                className={`flex flex-col gap-3 px-5 py-3 sm:flex-row sm:items-center ${
                  i > 0 ? "border-t" : ""
                }`}
              >
                <span className="w-8 shrink-0 text-sm font-semibold">
                  {d.label}
                </span>

                {/* 휴무 토글 */}
                <button
                  role="switch"
                  aria-checked={slot.off}
                  onClick={() =>
                    update(
                      d.key,
                      slot.off
                        ? { off: false, start: "09:00", end: "18:00" }
                        : { off: true, start: "", end: "" },
                    )
                  }
                  className={`flex w-24 shrink-0 items-center gap-2 rounded-full border px-1 py-1 text-xs font-medium transition-colors ${
                    slot.off
                      ? "border-free/30 bg-free-bg text-free"
                      : "bg-surface text-muted"
                  }`}
                >
                  <span
                    className={`size-5 rounded-full transition-transform ${
                      slot.off
                        ? "translate-x-0 bg-free"
                        : "translate-x-0 border bg-background"
                    }`}
                  />
                  {slot.off ? "휴무" : "근무일"}
                </button>

                {/* 근무 시간 */}
                {slot.off ? (
                  <span className="text-sm text-free">하루 종일 가능 🎉</span>
                ) : (
                  <span className="flex items-center gap-2 text-sm">
                    <input
                      type="time"
                      value={slot.start}
                      onChange={(e) => update(d.key, { start: e.target.value })}
                      className="rounded-lg border bg-surface px-2.5 py-1.5 text-sm tabular-nums focus:border-primary focus:outline-none"
                    />
                    <span className="text-muted">~</span>
                    <input
                      type="time"
                      value={slot.end}
                      onChange={(e) => update(d.key, { end: e.target.value })}
                      className="rounded-lg border bg-surface px-2.5 py-1.5 text-sm tabular-nums focus:border-primary focus:outline-none"
                    />
                  </span>
                )}
              </li>
            );
          })}
        </ul>

        <div className="flex items-center justify-between gap-3 border-t bg-background/50 px-5 py-4">
          <p className="text-xs text-muted">
            {saved
              ? "저장됐어요. 멤버들 화면에 바로 반영돼요. (목업 — 백엔드 연결 전)"
              : "수정 후 저장을 눌러 주세요."}
          </p>
          <button
            onClick={() => setSaved(true)}
            disabled={saved}
            className="inline-flex items-center gap-1.5 rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
          >
            {saved ? "저장됨 ✓" : "저장하기"}
          </button>
        </div>
      </section>
    </div>
  );
}
