"use client";

import Link from "next/link";
import type { CrewMember } from "@/lib/types";
import type { DayRecommendation } from "@/lib/types";
import { formatTime, initialOf, tintFor } from "@/lib/ui-helpers";

const DAY_LABELS: Record<DayRecommendation["dayOfWeek"], string> = {
  MONDAY: "월요일",
  TUESDAY: "화요일",
  WEDNESDAY: "수요일",
  THURSDAY: "목요일",
  FRIDAY: "금요일",
  SATURDAY: "토요일",
  SUNDAY: "일요일",
};

type Props = {
  recommendation: DayRecommendation | null;
  members: CrewMember[];
};

export function CommonFreeCard({ recommendation, members }: Props) {
  const title = recommendation
    ? recommendation.allDayFree
      ? `${DAY_LABELS[recommendation.dayOfWeek]}에 다 같이 비어요`
      : `${DAY_LABELS[recommendation.dayOfWeek]} ${formatTime(recommendation.availableFrom)} 이후, 다 같이 비어요`
    : "이번 주에는 다 같이 만날 시간이 부족해요";
  const note = recommendation
    ? recommendation.allDayFree
      ? `${recommendation.offCount}명 휴무 · 하루 종일 가능`
      : `${recommendation.offCount}명 휴무 · 근무가 일찍 끝나요`
    : "스케줄을 채워보면 추천이 나올 수 있어요.";

  return (
    <section className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
      <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3.5">
          <span className="mt-0.5 grid size-10 shrink-0 place-items-center rounded-full bg-amber-50 text-lg">
            ⭐
          </span>
          <div>
            <p className="text-xs font-medium text-star">이번 주 추천</p>
            <h2 className="mt-0.5 text-lg font-semibold tracking-tight">{title}</h2>
            <p className="mt-1 text-sm text-muted">{note}</p>
          </div>
        </div>

        <div className="flex items-center gap-4 sm:flex-col sm:items-end">
          <div className="flex -space-x-2">
            {members.map((m) => (
              <span
                key={m.userId}
                className={`grid size-8 place-items-center rounded-full border-2 border-surface text-xs font-semibold ${tintFor(m.userId)}`}
                title={m.nickname}
              >
                {initialOf(m.nickname)}
              </span>
            ))}
          </div>
          <Link
            href="/meetup"
            className="inline-flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:scale-[1.02] active:scale-95"
          >
            약속 잡기
            <span aria-hidden>→</span>
          </Link>
        </div>
      </div>
    </section>
  );
}
