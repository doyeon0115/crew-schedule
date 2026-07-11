"use client";

import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useCreatePoll } from "@/lib/poll-hooks";

type Candidate = { date: string; startTime: string };

const emptyCandidate = (): Candidate => ({ date: "", startTime: "" });

type Props = { crewId: number; onCreated?: () => void };

export function PollCreator({ crewId, onCreated }: Props) {
  const [title, setTitle] = useState("");
  const [candidates, setCandidates] = useState<Candidate[]>([emptyCandidate()]);
  const create = useCreatePoll(crewId);

  const patch = (idx: number, next: Partial<Candidate>) => {
    setCandidates((prev) => prev.map((c, i) => (i === idx ? { ...c, ...next } : c)));
  };
  const addRow = () => setCandidates((prev) => [...prev, emptyCandidate()]);
  const removeRow = (idx: number) =>
    setCandidates((prev) => (prev.length === 1 ? prev : prev.filter((_, i) => i !== idx)));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = candidates
      .filter((c) => c.date)
      .map((c) => ({ date: c.date, startTime: c.startTime ? `${c.startTime}:00` : null }));
    if (!title.trim() || cleaned.length === 0) return;
    create.mutate(
      { title: title.trim(), candidates: cleaned },
      {
        onSuccess: () => {
          setTitle("");
          setCandidates([emptyCandidate()]);
          onCreated?.();
        },
      },
    );
  };

  const errorMessage =
    create.error instanceof ApiError ? create.error.message : create.error ? "생성에 실패했어요." : null;

  return (
    <form
      onSubmit={submit}
      className="flex flex-col gap-4 rounded-[var(--radius-app)] border bg-surface p-5 shadow-sm"
    >
      <h3 className="text-sm font-semibold tracking-tight">새 날짜 투표</h3>
      <label className="flex flex-col gap-1.5">
        <span className="text-xs font-medium text-muted">제목</span>
        <input
          type="text"
          required
          maxLength={100}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="rounded-lg border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
          placeholder="예: 이번 주 저녁 언제 볼까?"
        />
      </label>

      <div className="flex flex-col gap-2">
        <span className="text-xs font-medium text-muted">후보 날짜 (최소 1개)</span>
        {candidates.map((c, i) => (
          <div key={i} className="flex flex-wrap items-center gap-2">
            <input
              type="date"
              required
              value={c.date}
              onChange={(e) => patch(i, { date: e.target.value })}
              className="rounded-lg border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
            />
            <input
              type="time"
              value={c.startTime}
              onChange={(e) => patch(i, { startTime: e.target.value })}
              className="rounded-lg border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
            />
            <span className="text-xs text-muted">(시간 비우면 미정)</span>
            {candidates.length > 1 && (
              <button
                type="button"
                onClick={() => removeRow(i)}
                className="rounded-full border px-3 py-1 text-xs text-muted hover:text-foreground"
              >
                삭제
              </button>
            )}
          </div>
        ))}
        <button
          type="button"
          onClick={addRow}
          className="self-start text-xs text-primary hover:underline"
        >
          + 후보 추가
        </button>
      </div>

      {errorMessage && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">{errorMessage}</p>
      )}

      <button
        type="submit"
        disabled={create.isPending}
        className="self-end rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground shadow-sm hover:scale-[1.01] active:scale-95 disabled:opacity-50"
      >
        {create.isPending ? "만드는 중..." : "투표 만들기"}
      </button>
    </form>
  );
}
