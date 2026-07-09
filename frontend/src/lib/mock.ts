// 정적 UI용 목업 데이터. 백엔드 WeeklySlot 모델(dayOfWeek/off/start/end)과 동일 구조.

export type DayKey = "mon" | "tue" | "wed" | "thu" | "fri" | "sat" | "sun";

export type Slot = { off: boolean; start: string; end: string };

export type Member = {
  id: string;
  name: string;
  initial: string;
  /** 아바타 배경(Tailwind 클래스) */
  tint: string;
  sched: Record<DayKey, Slot>;
};

export const DAYS: { key: DayKey; label: string }[] = [
  { key: "mon", label: "월" },
  { key: "tue", label: "화" },
  { key: "wed", label: "수" },
  { key: "thu", label: "목" },
  { key: "fri", label: "금" },
  { key: "sat", label: "토" },
  { key: "sun", label: "일" },
];

const W = (start: string, end: string): Slot => ({ off: false, start, end });
const OFF: Slot = { off: true, start: "", end: "" };

export const CREW = {
  name: "우리끼리",
  online: 4,
};

export const MEMBERS: Member[] = [
  {
    id: "p1",
    name: "지영",
    initial: "지",
    tint: "bg-indigo-100 text-indigo-700",
    sched: {
      mon: W("09:00", "18:00"),
      tue: W("09:00", "18:00"),
      wed: W("09:00", "18:00"),
      thu: OFF,
      fri: W("09:00", "18:00"),
      sat: OFF,
      sun: OFF,
    },
  },
  {
    id: "p2",
    name: "유진",
    initial: "유",
    tint: "bg-rose-100 text-rose-700",
    sched: {
      mon: OFF,
      tue: W("09:00", "20:30"),
      wed: W("09:00", "20:30"),
      thu: W("09:00", "17:00"),
      fri: W("09:00", "20:30"),
      sat: W("09:00", "20:30"),
      sun: OFF,
    },
  },
  {
    id: "p3",
    name: "선우",
    initial: "선",
    tint: "bg-emerald-100 text-emerald-700",
    sched: {
      mon: W("10:00", "19:00"),
      tue: W("10:00", "19:00"),
      wed: OFF,
      thu: W("10:00", "16:00"),
      fri: W("10:00", "19:00"),
      sat: OFF,
      sun: OFF,
    },
  },
  {
    id: "p4",
    name: "수민",
    initial: "수",
    tint: "bg-amber-100 text-amber-700",
    sched: {
      mon: OFF,
      tue: W("13:00", "22:00"),
      wed: W("13:00", "22:00"),
      thu: W("09:00", "15:00"),
      fri: OFF,
      sat: W("13:00", "22:00"),
      sun: W("13:00", "22:00"),
    },
  },
];

/** 요일별 '쉬는 사람 수' (그리드 강조용) */
export function offCount(dayKey: DayKey): number {
  return MEMBERS.filter((m) => m.sched[dayKey].off).length;
}

/** 다 같이 비는 시각 — 근무자 중 가장 늦게 끝나는 시간. 전원 휴무면 null(하루 종일). */
export function commonFreeFrom(dayKey: DayKey): string | null {
  const ends = MEMBERS.filter((m) => !m.sched[dayKey].off).map(
    (m) => m.sched[dayKey].end,
  );
  if (ends.length === 0) return null;
  return ends.reduce((a, b) => (a >= b ? a : b));
}

/** 추천 슬롯 (정적: 목요일 저녁 — 전원 근무가 일찍 끝나 다 같이 비는 시간대) */
export const RECOMMENDED = {
  dayKey: "thu" as DayKey,
  dayLabel: "목요일",
  timeLabel: "저녁 7시 이후",
  note: "모두 근무가 일찍 끝나요",
};
