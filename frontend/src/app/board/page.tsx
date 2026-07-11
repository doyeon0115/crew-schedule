"use client";

import Link from "next/link";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useCreatePost, useCrewPosts } from "@/lib/board-hooks";
import { useMyCrews } from "@/lib/schedule-hooks";
import { initialOf, tintFor } from "@/lib/ui-helpers";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ko-KR", { dateStyle: "short", timeStyle: "short" });
}

export default function BoardPage() {
  const crews = useMyCrews();
  const activeCrew = crews.data?.[0] ?? null;
  const posts = useCrewPosts(activeCrew?.id ?? null);
  const [showForm, setShowForm] = useState(false);

  if (crews.isLoading) return <p className="text-sm text-muted">불러오는 중...</p>;
  if (!activeCrew) {
    return (
      <p className="text-sm text-muted">
        크루가 있어야 게시판을 쓸 수 있어요. 홈에서 먼저 크루를 만드세요.
      </p>
    );
  }

  return (
    <>
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
            게시판 · {activeCrew.name}
          </h1>
          <p className="mt-1 text-sm text-muted">
            크루원들에게 공지·건의·이야기를 남겨보세요.
          </p>
        </div>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm hover:scale-[1.02] active:scale-95"
        >
          {showForm ? "닫기" : "새 글"}
        </button>
      </div>

      <div className="flex flex-col gap-5">
        {showForm && (
          <PostComposer crewId={activeCrew.id} onDone={() => setShowForm(false)} />
        )}

        {posts.isLoading ? (
          <p className="text-sm text-muted">게시글을 불러오는 중...</p>
        ) : !posts.data || posts.data.posts.length === 0 ? (
          <div className="rounded-[var(--radius-app)] border bg-surface p-8 text-center text-sm text-muted">
            아직 게시글이 없어요.
          </div>
        ) : (
          <ul className="flex flex-col gap-3">
            {posts.data.posts.map((p) => (
              <li
                key={p.id}
                className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm hover:border-primary/40"
              >
                <Link href={`/board/${p.id}`} className="block px-5 py-4">
                  <div className="flex items-center gap-2">
                    <span
                      className={`grid size-7 place-items-center rounded-full text-xs font-semibold ${tintFor(p.authorId)}`}
                    >
                      {initialOf(p.authorNickname)}
                    </span>
                    <span className="text-xs text-muted">
                      {p.authorNickname} · {formatDate(p.createdAt)}
                    </span>
                  </div>
                  <h2 className="mt-2 text-base font-semibold tracking-tight">{p.title}</h2>
                  <p className="mt-1 text-sm text-muted line-clamp-2">{p.excerpt}</p>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  );
}

function PostComposer({ crewId, onDone }: { crewId: number; onDone: () => void }) {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const create = useCreatePost(crewId);
  const errorMessage =
    create.error instanceof ApiError ? create.error.message : create.error ? "작성에 실패했어요." : null;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    create.mutate(
      { title: title.trim(), content: content.trim() },
      {
        onSuccess: () => {
          setTitle("");
          setContent("");
          onDone();
        },
      },
    );
  };

  return (
    <form
      onSubmit={submit}
      className="flex flex-col gap-3 rounded-[var(--radius-app)] border bg-surface p-5 shadow-sm"
    >
      <input
        type="text"
        required
        maxLength={100}
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="제목"
        className="rounded-lg border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
      />
      <textarea
        required
        maxLength={20000}
        rows={5}
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="내용"
        className="rounded-lg border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
      />
      {errorMessage && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">{errorMessage}</p>
      )}
      <button
        type="submit"
        disabled={create.isPending || !title.trim() || !content.trim()}
        className="self-end rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground shadow-sm hover:scale-[1.01] active:scale-95 disabled:opacity-50"
      >
        {create.isPending ? "작성 중..." : "올리기"}
      </button>
    </form>
  );
}
