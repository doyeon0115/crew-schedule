import Link from "next/link";
import { MEMBERS, RECOMMENDED } from "@/lib/mock";

export function CommonFreeCard() {
  return (
    <section className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
      <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3.5">
          <span className="mt-0.5 grid size-10 shrink-0 place-items-center rounded-full bg-amber-50 text-lg">
            ⭐
          </span>
          <div>
            <p className="text-xs font-medium text-star">이번 주 추천</p>
            <h2 className="mt-0.5 text-lg font-semibold tracking-tight">
              {RECOMMENDED.dayLabel} {RECOMMENDED.timeLabel}, 다 같이 비어요
            </h2>
            <p className="mt-1 text-sm text-muted">{RECOMMENDED.note}</p>
          </div>
        </div>

        <div className="flex items-center gap-4 sm:flex-col sm:items-end">
          <div className="flex -space-x-2">
            {MEMBERS.map((m) => (
              <span
                key={m.id}
                className={`grid size-8 place-items-center rounded-full border-2 border-surface text-xs font-semibold ${m.tint}`}
                title={m.name}
              >
                {m.initial}
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
