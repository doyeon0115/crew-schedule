"use client";

import { ApiError } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import {
  useCancelMeetup,
  useConfirmMeetup,
  useCrewMeetups,
  useJoinMeetup,
  useRespondMeetup,
} from "@/lib/meetup-hooks";
import type { Meetup, Rsvp } from "@/lib/types";
import { formatTime, initialOf, tintFor } from "@/lib/ui-helpers";

const STATUS_LABEL: Record<Meetup["status"], { label: string; cls: string }> = {
  PROPOSED: { label: "제안 중", cls: "bg-primary/10 text-primary" },
  CONFIRMED: { label: "확정", cls: "bg-emerald-100 text-emerald-700" },
  CANCELED: { label: "취소", cls: "bg-neutral-100 text-muted" },
};

const RSVP_LABEL: Record<Rsvp, string> = {
  PENDING: "미응답",
  ATTEND: "참석",
  MAYBE: "미정",
  ABSENT: "불참",
};

const RSVP_BTN: { rsvp: Rsvp; label: string; cls: string }[] = [
  { rsvp: "ATTEND", label: "참석", cls: "border-emerald-300 bg-emerald-50 text-emerald-700" },
  { rsvp: "MAYBE", label: "미정", cls: "border-amber-300 bg-amber-50 text-amber-700" },
  { rsvp: "ABSENT", label: "불참", cls: "border-red-300 bg-red-50 text-red-700" },
];

type Props = { crewId: number };

export function MeetupList({ crewId }: Props) {
  const me = useAuthStore((s) => s.user);
  const { data, isLoading } = useCrewMeetups(crewId);
  const respond = useRespondMeetup(crewId);
  const confirm = useConfirmMeetup(crewId);
  const cancel = useCancelMeetup(crewId);
  const join = useJoinMeetup(crewId);

  if (isLoading) return <p className="text-sm text-muted">약속을 불러오는 중...</p>;
  if (!data || data.length === 0) {
    return (
      <div className="rounded-[var(--radius-app)] border bg-surface p-6 text-center text-sm text-muted">
        아직 만들어진 약속이 없어요.
      </div>
    );
  }

  const errorMessage = (() => {
    for (const m of [respond, confirm, cancel, join]) {
      if (m.error instanceof ApiError) return m.error.message;
    }
    return null;
  })();

  return (
    <div className="flex flex-col gap-3">
      {errorMessage && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">{errorMessage}</p>
      )}
      {data.map((m) => (
        <MeetupCard
          key={m.id}
          meetup={m}
          myUserId={me?.id}
          onRespond={(rsvp) => respond.mutate({ meetupId: m.id, rsvp })}
          onConfirm={() => confirm.mutate(m.id)}
          onCancel={() => cancel.mutate(m.id)}
          onJoin={() => join.mutate(m.id)}
        />
      ))}
    </div>
  );
}

type CardProps = {
  meetup: Meetup;
  myUserId: number | undefined;
  onRespond: (rsvp: Rsvp) => void;
  onConfirm: () => void;
  onCancel: () => void;
  onJoin: () => void;
};

function MeetupCard({ meetup, myUserId, onRespond, onConfirm, onCancel, onJoin }: CardProps) {
  const isCreator = myUserId === meetup.creatorId;
  const myParticipant = meetup.participants.find((p) => p.userId === myUserId);
  const status = STATUS_LABEL[meetup.status];
  const isProposed = meetup.status === "PROPOSED";
  const isFlash = meetup.capacity !== null;
  const isFull = isFlash && meetup.capacity !== null && meetup.currentParticipants >= meetup.capacity;

  return (
    <article className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
      <div className="flex flex-col gap-2 p-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <h3 className="text-base font-semibold tracking-tight">{meetup.title}</h3>
            <span className={`rounded-full px-2 py-0.5 text-[11px] font-bold ${status.cls}`}>
              {status.label}
            </span>
            {isFlash && (
              <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[11px] font-bold text-amber-800">
                번개 {meetup.currentParticipants}/{meetup.capacity}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-muted">
            {meetup.meetDate} · {formatTime(meetup.startTime)}
            {meetup.location ? ` · ${meetup.location}` : ""}
          </p>
          {meetup.memo && <p className="mt-1 text-xs text-muted">{meetup.memo}</p>}
        </div>
      </div>

      {/* 참여자 목록 */}
      {meetup.participants.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 border-t px-4 py-3">
          {meetup.participants.map((p) => (
            <div key={p.userId} className="flex items-center gap-1.5">
              <span
                className={`grid size-6 place-items-center rounded-full text-[10px] font-semibold ${tintFor(p.userId)}`}
              >
                {initialOf(p.nickname)}
              </span>
              <span className="text-xs">
                <span className="font-medium">{p.nickname}</span>
                <span className="text-muted"> · {RSVP_LABEL[p.rsvp]}</span>
              </span>
            </div>
          ))}
        </div>
      )}

      {/* 액션 영역 */}
      <div className="flex flex-wrap items-center justify-end gap-2 border-t bg-background/50 px-4 py-3">
        {/* Flash join */}
        {isFlash && isProposed && !myParticipant && (
          <button
            onClick={onJoin}
            disabled={isFull}
            className="rounded-full bg-primary px-4 py-1.5 text-xs font-semibold text-primary-foreground shadow-sm hover:scale-[1.02] active:scale-95 disabled:opacity-40"
          >
            {isFull ? "정원 마감" : "번개 참여"}
          </button>
        )}

        {/* RSVP: 참여자만 응답 가능. flash에서는 join으로 이미 ATTEND 되어 있음 */}
        {isProposed && myParticipant && !isFlash && (
          <div className="flex gap-1.5">
            {RSVP_BTN.map((b) => {
              const active = myParticipant.rsvp === b.rsvp;
              return (
                <button
                  key={b.rsvp}
                  onClick={() => onRespond(b.rsvp)}
                  className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                    active ? b.cls : "text-muted hover:text-foreground"
                  }`}
                >
                  {b.label}
                </button>
              );
            })}
          </div>
        )}

        {/* Creator actions */}
        {isCreator && isProposed && (
          <div className="flex gap-1.5">
            <button
              onClick={onConfirm}
              className="rounded-full border border-emerald-300 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-100"
            >
              확정
            </button>
            <button
              onClick={onCancel}
              className="rounded-full border px-3 py-1 text-xs font-medium text-muted hover:text-foreground"
            >
              취소
            </button>
          </div>
        )}
      </div>
    </article>
  );
}
