"use client";

import { useEffect, useRef, useState } from "react";
import {
  useMarkAllRead,
  useNotifications,
  useNotificationSocket,
} from "@/lib/notification-hooks";
import type { NotificationItem } from "@/lib/types";

const TYPE_LABELS: Record<NotificationItem["type"], string> = {
  MEETUP_PROPOSED: "새 약속 제안",
  MEETUP_JOINED: "번개 참여자 도착",
  MEETUP_CONFIRMED: "약속 확정",
  POLL_CREATED: "새 날짜 투표",
  POLL_CLOSED: "투표 마감",
};

function summarize(item: NotificationItem): string {
  try {
    const p = JSON.parse(item.payload);
    if (p.title) return String(p.title);
    if (p.winnerDate) return `winner: ${p.winnerDate}`;
    return "";
  } catch {
    return "";
  }
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

export function NotificationBell() {
  useNotificationSocket();
  const { data } = useNotifications();
  const markAllRead = useMarkAllRead();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  // 바깥 클릭 시 닫기
  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [open]);

  const unread = data?.unreadCount ?? 0;

  return (
    <div ref={rootRef} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        aria-label="알림"
        className="relative rounded-full border px-2.5 py-1.5 text-muted transition-colors hover:border-primary/40 hover:text-foreground"
      >
        <BellIcon />
        {unread > 0 && (
          <span className="absolute -right-1 -top-1 grid min-w-[18px] place-items-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
            {unread > 99 ? "99+" : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-20 mt-2 w-80 overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-lg">
          <div className="flex items-center justify-between gap-2 border-b px-4 py-2.5">
            <span className="text-sm font-semibold">알림</span>
            <button
              onClick={() => markAllRead.mutate()}
              disabled={unread === 0 || markAllRead.isPending}
              className="text-xs text-muted hover:text-foreground disabled:opacity-40"
            >
              모두 읽음
            </button>
          </div>
          <div className="max-h-96 overflow-y-auto">
            {!data || data.notifications.length === 0 ? (
              <p className="px-4 py-6 text-center text-sm text-muted">아직 알림이 없어요.</p>
            ) : (
              <ul>
                {data.notifications.map((n) => (
                  <li
                    key={n.id}
                    className={`flex flex-col gap-1 border-b px-4 py-3 last:border-b-0 ${
                      n.read ? "" : "bg-primary/5"
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs font-semibold text-primary">
                        {TYPE_LABELS[n.type] ?? n.type}
                      </span>
                      <span className="text-[10px] text-muted">{formatTime(n.createdAt)}</span>
                    </div>
                    <p className="text-sm">{summarize(n)}</p>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function BellIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M6 8a6 6 0 1112 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 003.4 0" />
    </svg>
  );
}
