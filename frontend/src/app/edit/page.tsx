"use client";

import { ScheduleEditor } from "@/components/ScheduleEditor";
import { useMyCrews } from "@/lib/schedule-hooks";

export default function EditPage() {
  const crews = useMyCrews();
  const crewName = crews.data?.[0]?.name;

  return (
    <>
      <div className="mb-5">
        <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
          스케줄 수정{crewName ? ` · ${crewName}` : ""}
        </h1>
        <p className="mt-1 text-sm text-muted">
          이번 주 근무·휴무를 입력하면 추천 시간이 자동으로 갱신돼요.
        </p>
      </div>

      <ScheduleEditor />
    </>
  );
}
