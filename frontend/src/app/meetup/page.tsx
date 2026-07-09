import type { Metadata } from "next";
import { MeetupPlanner } from "@/components/MeetupPlanner";
import { CREW } from "@/lib/mock";

export const metadata: Metadata = {
  title: "약속 잡기 — Crew Schedule",
};

export default function MeetupPage() {
  return (
    <>
      <div className="mb-5">
        <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
          약속 잡기 · {CREW.name}
        </h1>
        <p className="mt-1 text-sm text-muted">
          요일과 시간을 고르면 멤버들에게 바로 제안할 수 있어요.
        </p>
      </div>

      <MeetupPlanner />
    </>
  );
}
