"use client";

import { useEffect, useMemo, useState } from "react";
import { ApiError } from "@/lib/api";
import { useMyCrews, useMySchedule, useUpdateMySchedule } from "@/lib/schedule-hooks";
import type { SlotResponse, SlotUpdate } from "@/lib/types";
import { DAYS, formatTime, indexByDay, type DayKey } from "@/lib/ui-helpers";

type DraftSlot = { off: boolean; start: string; end: string };
type WeekDraft = Record<DayKey, DraftSlot>;

function toDraft(index: Record<DayKey, SlotResponse>): WeekDraft {
  const draft: Partial<WeekDraft> = {};
  for (const d of DAYS) {
    const s = index[d.key];
    draft[d.key] = {
      off: s.off,
      start: formatTime(s.startTime) || "09:00",
      end: formatTime(s.endTime) || "18:00",
    };
  }
  return draft as WeekDraft;
}

function toUpdates(draft: WeekDraft): SlotUpdate[] {
  return DAYS.map((d) => ({
    dayOfWeek: d.iso,
    off: draft[d.key].off,
    startTime: draft[d.key].off ? null : `${draft[d.key].start}:00`,
    endTime: draft[d.key].off ? null : `${draft[d.key].end}:00`,
  }));
}

export function ScheduleEditor() {
  const mySchedule = useMySchedule();
  const crews = useMyCrews();
  const crewId = crews.data?.[0]?.id ?? null;
  const update = useUpdateMySchedule(crewId);

  const indexed = useMemo(
    () => (mySchedule.data ? indexByDay(mySchedule.data.slots) : null),
    [mySchedule.data],
  );

  const [draft, setDraft] = useState<WeekDraft | null>(null);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    if (indexed) {
      setDraft(toDraft(indexed));
      setDirty(false);
    }
  }, [indexed]);

  if (mySchedule.isLoading || !draft) {
    return <p className="text-sm text-muted">스케줄을 불러오는 중이에요...</p>;
  }

  const patch = (key: DayKey, next: Partial<DraftSlot>) => {
    setDraft((prev) => (prev ? { ...prev, [key]: { ...prev[key], ...next } } : prev));
    setDirty(true);
    update.reset();
  };

  const errorMessage =
    update.error instanceof ApiError ? update.error.message : update.error ? "저장에 실패했어요." : null;

  const save = () => update.mutate(toUpdates(draft));

  return (
    <div className="flex flex-col gap-5">
      <section className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
        <div className="flex items-center justify-between gap-3 border-b px-5 py-3.5">
          <h3 className="text-sm font-semibold tracking-tight">이번 주 내 근무</h3>
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
                <span className="w-8 shrink-0 text-sm font-semibold">{d.label}</span>

                <button
                  role="switch"
                  aria-checked={slot.off}
                  onClick={() => patch(d.key, { off: !slot.off })}
                  className={`flex w-24 shrink-0 items-center gap-2 rounded-full border px-1 py-1 text-xs font-medium transition-colors ${
                    slot.off
                      ? "border-free/30 bg-free-bg text-free"
                      : "bg-surface text-muted"
                  }`}
                >
                  <span
                    className={`size-5 rounded-full transition-transform ${
                      slot.off ? "translate-x-0 bg-free" : "translate-x-0 border bg-background"
                    }`}
                  />
                  {slot.off ? "휴무" : "근무일"}
                </button>

                {slot.off ? (
                  <span className="text-sm text-free">하루 종일 가능 🎉</span>
                ) : (
                  <span className="flex items-center gap-2 text-sm">
                    <input
                      type="time"
                      value={slot.start}
                      onChange={(e) => patch(d.key, { start: e.target.value })}
                      className="rounded-lg border bg-surface px-2.5 py-1.5 text-sm tabular-nums focus:border-primary focus:outline-none"
                    />
                    <span className="text-muted">~</span>
                    <input
                      type="time"
                      value={slot.end}
                      onChange={(e) => patch(d.key, { end: e.target.value })}
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
            {errorMessage
              ? errorMessage
              : update.isSuccess && !dirty
                ? "저장됐어요. 크루원들 화면에 바로 반영돼요."
                : "수정 후 저장을 눌러 주세요."}
          </p>
          <button
            onClick={save}
            disabled={!dirty || update.isPending}
            className="inline-flex items-center gap-1.5 rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
          >
            {update.isPending ? "저장 중..." : update.isSuccess && !dirty ? "저장됨 ✓" : "저장하기"}
          </button>
        </div>
      </section>
    </div>
  );
}
