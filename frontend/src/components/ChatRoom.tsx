"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useAuthStore } from "@/lib/auth-store";
import { useChatHistory, useChatSocket } from "@/lib/chat-hooks";
import type { ChatMessage } from "@/lib/types";
import { initialOf, tintFor } from "@/lib/ui-helpers";

function formatTime(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

type Props = { crewId: number };

export function ChatRoom({ crewId }: Props) {
  const me = useAuthStore((s) => s.user);
  const history = useChatHistory(crewId);
  const { status, liveMessages, send } = useChatSocket(crewId);
  const [draft, setDraft] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  // 히스토리(오래된 → 최신) + 실시간 append. id 중복 제거.
  const messages = useMemo(() => {
    const seen = new Set<number>();
    const list: ChatMessage[] = [];
    for (const m of history.data?.messages ?? []) {
      if (!seen.has(m.id)) {
        seen.add(m.id);
        list.push(m);
      }
    }
    for (const m of liveMessages) {
      if (!seen.has(m.id)) {
        seen.add(m.id);
        list.push(m);
      }
    }
    return list;
  }, [history.data, liveMessages]);

  useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages.length]);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const content = draft.trim();
    if (!content) return;
    if (send(content)) {
      setDraft("");
    }
  };

  const statusBadge =
    status === "connected"
      ? { label: "실시간 연결됨", cls: "bg-emerald-50 text-emerald-700" }
      : status === "connecting"
        ? { label: "연결 중...", cls: "bg-amber-50 text-amber-700" }
        : status === "closed"
          ? { label: "연결 끊김 - 재시도 중", cls: "bg-red-50 text-red-700" }
          : { label: "대기", cls: "bg-neutral-100 text-muted" };

  return (
    <section className="flex flex-col overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
      <div className="flex items-center justify-between gap-3 border-b px-5 py-3">
        <h3 className="text-sm font-semibold tracking-tight">채팅</h3>
        <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${statusBadge.cls}`}>
          {statusBadge.label}
        </span>
      </div>

      <div
        ref={scrollRef}
        className="h-[420px] overflow-y-auto bg-background/60 p-4"
      >
        {history.isLoading ? (
          <p className="text-center text-sm text-muted">불러오는 중...</p>
        ) : messages.length === 0 ? (
          <p className="text-center text-sm text-muted">아직 대화가 없어요. 첫 메시지를 남겨보세요.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {messages.map((m) => (
              <MessageBubble key={m.id} message={m} isMe={m.senderId === me?.id} />
            ))}
          </ul>
        )}
      </div>

      <form onSubmit={submit} className="flex items-center gap-2 border-t p-3">
        <input
          type="text"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={status === "connected" ? "메시지 입력..." : "연결을 기다리는 중..."}
          disabled={status !== "connected"}
          maxLength={2000}
          className="flex-1 rounded-full border bg-surface px-4 py-2 text-sm focus:border-primary focus:outline-none disabled:opacity-50"
        />
        <button
          type="submit"
          disabled={!draft.trim() || status !== "connected"}
          className="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
        >
          전송
        </button>
      </form>
    </section>
  );
}

function MessageBubble({ message, isMe }: { message: ChatMessage; isMe: boolean }) {
  return (
    <li className={`flex items-start gap-2 ${isMe ? "flex-row-reverse" : ""}`}>
      <span
        className={`grid size-8 shrink-0 place-items-center rounded-full text-xs font-semibold ${tintFor(message.senderId)}`}
      >
        {initialOf(message.senderNickname)}
      </span>
      <div className={`flex flex-col ${isMe ? "items-end" : "items-start"}`}>
        <span className="text-[11px] text-muted">
          {isMe ? "" : `${message.senderNickname} · `}
          {formatTime(message.sentAt)}
        </span>
        <div
          className={`mt-0.5 max-w-[75ch] rounded-2xl px-3.5 py-2 text-sm shadow-sm ${
            isMe ? "bg-primary text-primary-foreground" : "bg-surface"
          }`}
        >
          {message.content}
        </div>
      </div>
    </li>
  );
}
