import type { Metadata } from "next";
import { ScheduleEditor } from "@/components/ScheduleEditor";
import { CREW } from "@/lib/mock";

export const metadata: Metadata = {
  title: "스케줄 수정 — Crew Schedule",
};

export default function EditPage() {
  return (
    <>
      <div className="mb-5">
        <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
          스케줄 수정 · {CREW.name}
        </h1>
        <p className="mt-1 text-sm text-muted">
          이번 주 근무·휴무를 입력하면 추천 시간이 자동으로 갱신돼요.
        </p>
      </div>

      <ScheduleEditor />
    </>
  );
}
