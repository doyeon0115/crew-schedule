"use client";

import { useState } from "react";
import { ApiError } from "@/lib/api";
import {
  useAdminUsers,
  useDemoteUser,
  usePromoteUser,
  useReactivateUser,
  useSuspendUser,
} from "@/lib/admin-hooks";
import { useAuthStore } from "@/lib/auth-store";
import type { AdminUser } from "@/lib/types";
import { initialOf, tintFor } from "@/lib/ui-helpers";

export default function AdminUsersPage() {
  const [query, setQuery] = useState("");
  const [committed, setCommitted] = useState("");
  const { data, isLoading } = useAdminUsers(committed);
  const me = useAuthStore((s) => s.user);

  const suspend = useSuspendUser();
  const reactivate = useReactivateUser();
  const promote = usePromoteUser();
  const demote = useDemoteUser();

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    setCommitted(query.trim());
  };

  return (
    <section className="flex flex-col gap-4">
      <form onSubmit={submit} className="flex gap-2">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="이메일 또는 닉네임 검색"
          className="flex-1 rounded-lg border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
        />
        <button
          type="submit"
          className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm hover:scale-[1.01] active:scale-95"
        >
          검색
        </button>
      </form>

      <div className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
        {isLoading ? (
          <p className="p-8 text-center text-sm text-muted">불러오는 중...</p>
        ) : !data || data.users.length === 0 ? (
          <p className="p-8 text-center text-sm text-muted">유저가 없어요.</p>
        ) : (
          <ul>
            {data.users.map((u, i) => (
              <UserRow
                key={u.id}
                user={u}
                isMe={me?.id === u.id}
                isFirst={i === 0}
                onSuspend={() => suspend.mutate(u.id)}
                onReactivate={() => reactivate.mutate(u.id)}
                onPromote={() => promote.mutate(u.id)}
                onDemote={() => demote.mutate(u.id)}
                error={
                  [suspend, reactivate, promote, demote]
                    .map((m) => (m.error instanceof ApiError ? m.error.message : null))
                    .filter(Boolean)[0] ?? null
                }
              />
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}

type RowProps = {
  user: AdminUser;
  isMe: boolean;
  isFirst: boolean;
  onSuspend: () => void;
  onReactivate: () => void;
  onPromote: () => void;
  onDemote: () => void;
  error: string | null;
};

function UserRow({
  user,
  isMe,
  isFirst,
  onSuspend,
  onReactivate,
  onPromote,
  onDemote,
}: RowProps) {
  const isSuspended = user.status === "SUSPENDED";
  const isAdmin = user.role === "ADMIN";

  return (
    <li
      className={`flex flex-col gap-3 px-5 py-3 sm:flex-row sm:items-center sm:justify-between ${
        isFirst ? "" : "border-t"
      }`}
    >
      <div className="flex items-center gap-3 min-w-0">
        <span
          className={`grid size-9 shrink-0 place-items-center rounded-full text-sm font-semibold ${tintFor(user.id)}`}
        >
          {initialOf(user.nickname)}
        </span>
        <div className="min-w-0">
          <p className="flex items-center gap-2 text-sm font-semibold truncate">
            {user.nickname}
            {isAdmin && (
              <span className="rounded-full bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-primary">
                ADMIN
              </span>
            )}
            {isSuspended && (
              <span className="rounded-full bg-red-100 px-1.5 py-0.5 text-[10px] font-bold text-red-700">
                정지
              </span>
            )}
          </p>
          <p className="truncate text-xs text-muted">{user.email}</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {isSuspended ? (
          <button
            onClick={onReactivate}
            className="rounded-full border border-emerald-300 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-100"
          >
            해제
          </button>
        ) : (
          <button
            onClick={onSuspend}
            disabled={isMe}
            className="rounded-full border border-red-300 bg-red-50 px-3 py-1 text-xs font-medium text-red-700 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-40"
          >
            정지
          </button>
        )}
        {isAdmin ? (
          <button
            onClick={onDemote}
            disabled={isMe}
            className="rounded-full border px-3 py-1 text-xs font-medium text-muted hover:text-foreground disabled:cursor-not-allowed disabled:opacity-40"
          >
            강등
          </button>
        ) : (
          <button
            onClick={onPromote}
            className="rounded-full border px-3 py-1 text-xs font-medium text-primary hover:bg-primary/5"
          >
            관리자로
          </button>
        )}
      </div>
    </li>
  );
}
