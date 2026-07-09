import { CommonFreeCard } from "@/components/CommonFreeCard";
import { ScheduleGrid } from "@/components/ScheduleGrid";
import { CREW } from "@/lib/mock";

export default function Home() {
  return (
    <>
      <div className="mb-5">
        <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
          이번 주 · {CREW.name}
        </h1>
        <p className="mt-1 text-sm text-muted">
          다 같이 만날 수 있는 시간을 자동으로 찾아드려요.
        </p>
      </div>

      <div className="flex flex-col gap-5">
        <CommonFreeCard />
        <ScheduleGrid />
      </div>
    </>
  );
}
