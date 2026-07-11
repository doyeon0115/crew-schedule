"use client";

import { MeetupPlanner } from "@/components/MeetupPlanner";
import { useMyCrews } from "@/lib/schedule-hooks";

export default function MeetupPage() {
  const crews = useMyCrews();
  const crewName = crews.data?.[0]?.name;

  return (
    <>
      <div className="mb-5">
        <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
          약속 잡기{crewName ? ` · ${crewName}` : ""}
        </h1>
        <p className="mt-1 text-sm text-muted">
          요일과 시간을 고르면 멤버들에게 바로 제안할 수 있어요.
        </p>
      </div>

      <MeetupPlanner />
    </>
  );
}
