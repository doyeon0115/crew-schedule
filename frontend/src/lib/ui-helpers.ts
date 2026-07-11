import type { DayOfWeek, MemberSchedule, SlotResponse } from "./types";

export type DayKey = "mon" | "tue" | "wed" | "thu" | "fri" | "sat" | "sun";

export const DAYS: { key: DayKey; label: string; iso: DayOfWeek }[] = [
  { key: "mon", label: "월", iso: "MONDAY" },
  { key: "tue", label: "화", iso: "TUESDAY" },
  { key: "wed", label: "수", iso: "WEDNESDAY" },
  { key: "thu", label: "목", iso: "THURSDAY" },
  { key: "fri", label: "금", iso: "FRIDAY" },
  { key: "sat", label: "토", iso: "SATURDAY" },
  { key: "sun", label: "일", iso: "SUNDAY" },
];

/** 결정론적으로 유저에게 색상 팔레트를 배정. 색상은 시안(mock)과 동일 톤. */
const TINTS = [
  "bg-indigo-100 text-indigo-700",
  "bg-rose-100 text-rose-700",
  "bg-emerald-100 text-emerald-700",
  "bg-amber-100 text-amber-700",
  "bg-sky-100 text-sky-700",
  "bg-violet-100 text-violet-700",
];

export function tintFor(userId: number): string {
  return TINTS[userId % TINTS.length];
}

export function initialOf(nickname: string): string {
  return nickname.slice(0, 1);
}

/** 백엔드 "HH:mm:ss"를 UI 표시용 "HH:mm"으로. */
export function formatTime(time: string | null): string {
  if (!time) return "";
  return time.slice(0, 5);
}

/** 요일 iso → key 매핑 */
export function keyOf(day: DayOfWeek): DayKey {
  return DAYS.find((d) => d.iso === day)!.key;
}

/** MemberSchedule의 slot을 요일 key로 인덱싱. 스케줄 미입력 요일은 자동 휴무 취급. */
export function indexByDay(
  slots: SlotResponse[],
): Record<DayKey, SlotResponse> {
  const map = new Map<DayKey, SlotResponse>();
  for (const s of slots) {
    map.set(keyOf(s.dayOfWeek), s);
  }
  const result: Partial<Record<DayKey, SlotResponse>> = {};
  for (const d of DAYS) {
    result[d.key] =
      map.get(d.key) ?? {
        dayOfWeek: d.iso,
        off: true,
        startTime: null,
        endTime: null,
      };
  }
  return result as Record<DayKey, SlotResponse>;
}

/** 크루 보드에서 요일별 '쉬는 사람 수' */
export function offCountForDay(
  members: MemberSchedule[],
  dayKey: DayKey,
): number {
  return members.reduce(
    (n, m) => (indexByDay(m.slots)[dayKey].off ? n + 1 : n),
    0,
  );
}
