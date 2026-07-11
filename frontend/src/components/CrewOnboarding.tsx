"use client";

import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useCreateCrew, useJoinCrew } from "@/lib/schedule-hooks";

/** 로그인 직후 아직 소속 크루가 없는 유저에게 만들기 or 초대코드로 가입 안내. */
export function CrewOnboarding() {
  const [mode, setMode] = useState<"create" | "join">("create");
  const [name, setName] = useState("");
  const [inviteCode, setInviteCode] = useState("");
  const createCrew = useCreateCrew();
  const joinCrew = useJoinCrew();
  const mutation = mode === "create" ? createCrew : joinCrew;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (mode === "create") {
      createCrew.mutate({ name });
    } else {
      joinCrew.mutate({ inviteCode });
    }
  };

  const errorMessage =
    mutation.error instanceof ApiError ? mutation.error.message : mutation.error ? "요청에 실패했어요." : null;

  return (
    <div className="mx-auto max-w-md py-8">
      <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
        먼저 크루를 만드시거나 가입해 주세요
      </h1>
      <p className="mt-1 text-sm text-muted">
        크루가 있어야 함께 스케줄을 모을 수 있어요.
      </p>

      <div className="mt-5 inline-flex rounded-full border bg-surface p-1">
        <button
          type="button"
          onClick={() => setMode("create")}
          className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
            mode === "create" ? "bg-primary text-primary-foreground" : "text-muted"
          }`}
        >
          새로 만들기
        </button>
        <button
          type="button"
          onClick={() => setMode("join")}
          className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
            mode === "join" ? "bg-primary text-primary-foreground" : "text-muted"
          }`}
        >
          초대 코드로 가입
        </button>
      </div>

      <form onSubmit={submit} className="mt-5 flex flex-col gap-4">
        {mode === "create" ? (
          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-muted">크루 이름</span>
            <input
              type="text"
              required
              maxLength={50}
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="rounded-lg border bg-surface px-3 py-2.5 text-sm focus:border-primary focus:outline-none"
              placeholder="예: 우리끼리"
            />
          </label>
        ) : (
          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-muted">초대 코드</span>
            <input
              type="text"
              required
              maxLength={12}
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
              className="rounded-lg border bg-surface px-3 py-2.5 text-sm uppercase tracking-widest focus:border-primary focus:outline-none"
              placeholder="ABCD1234"
            />
          </label>
        )}

        {errorMessage && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">
            {errorMessage}
          </p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:scale-[1.01] active:scale-95 disabled:opacity-50 disabled:hover:scale-100"
        >
          {mutation.isPending
            ? "처리 중..."
            : mode === "create"
              ? "만들기"
              : "가입하기"}
        </button>
      </form>
    </div>
  );
}
