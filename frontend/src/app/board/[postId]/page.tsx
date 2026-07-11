"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import {
  useAddComment,
  useDeleteComment,
  useDeletePost,
  usePost,
  useReport,
  useToggleCommentReaction,
  useTogglePostReaction,
} from "@/lib/board-hooks";
import type { CommentNode } from "@/lib/types";
import { initialOf, tintFor } from "@/lib/ui-helpers";

const QUICK_EMOJIS = ["👍", "❤️", "😂", "🎉", "😮"];

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ko-KR", { dateStyle: "short", timeStyle: "short" });
}

export default function PostDetailPage() {
  const params = useParams<{ postId: string }>();
  const postId = Number(params?.postId);
  const router = useRouter();
  const me = useAuthStore((s) => s.user);
  const { data: post, isLoading, error } = usePost(Number.isFinite(postId) ? postId : null);
  const togglePostReaction = useTogglePostReaction(postId);
  const deletePost = useDeletePost(post?.crewId ?? null);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);

  if (isLoading) return <p className="text-sm text-muted">불러오는 중...</p>;
  if (error || !post) {
    return (
      <div className="text-sm text-muted">
        게시글을 찾을 수 없어요. <Link href="/board" className="text-primary">목록으로</Link>
      </div>
    );
  }

  const isAuthor = me?.id === post.authorId;

  return (
    <>
      <div className="mb-3">
        <Link href="/board" className="text-xs text-primary hover:underline">
          ← 목록
        </Link>
      </div>

      <article className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
        <div className="flex items-center gap-2 border-b px-5 py-3">
          <span
            className={`grid size-8 place-items-center rounded-full text-xs font-semibold ${tintFor(post.authorId)}`}
          >
            {initialOf(post.authorNickname)}
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold">{post.authorNickname}</p>
            <p className="text-[11px] text-muted">{formatDate(post.createdAt)}</p>
          </div>
          {isAuthor && (
            <button
              onClick={() => {
                if (confirm("삭제할까요?")) {
                  deletePost.mutate(post.id, { onSuccess: () => router.push("/board") });
                }
              }}
              className="text-xs text-muted hover:text-red-700"
            >
              삭제
            </button>
          )}
        </div>
        <div className="px-5 py-4">
          <h1 className="text-xl font-bold tracking-tight">{post.title}</h1>
          <p className="mt-3 whitespace-pre-wrap text-sm">{post.content}</p>
        </div>

        {/* 반응 */}
        <div className="flex items-center gap-2 border-t bg-background/50 px-4 py-2">
          {post.reactions.map((r) => (
            <button
              key={r.emoji}
              onClick={() => togglePostReaction.mutate(r.emoji)}
              className={`rounded-full border px-2.5 py-1 text-xs transition-colors ${
                r.myReaction
                  ? "border-primary bg-primary/10 text-primary"
                  : "bg-surface hover:border-primary/40"
              }`}
            >
              {r.emoji} {r.count}
            </button>
          ))}
          <button
            onClick={() => setShowEmojiPicker((v) => !v)}
            className="rounded-full border px-2.5 py-1 text-xs text-muted hover:text-foreground"
          >
            +
          </button>
          {showEmojiPicker && (
            <div className="flex gap-1">
              {QUICK_EMOJIS.map((e) => (
                <button
                  key={e}
                  onClick={() => {
                    togglePostReaction.mutate(e);
                    setShowEmojiPicker(false);
                  }}
                  className="rounded-full border bg-surface px-2 py-1 text-sm hover:border-primary/40"
                >
                  {e}
                </button>
              ))}
            </div>
          )}
          <ReportButton className="ml-auto" targetType="POST" targetId={post.id} />
        </div>
      </article>

      {/* 댓글 */}
      <section className="mt-5">
        <h2 className="mb-3 text-sm font-semibold tracking-tight">
          댓글 {post.comments.reduce((n, c) => n + 1 + c.replies.length, 0)}
        </h2>
        <CommentComposer postId={post.id} />
        <ul className="mt-3 flex flex-col gap-3">
          {post.comments.map((c) => (
            <CommentItem key={c.id} comment={c} postId={post.id} depth={0} />
          ))}
        </ul>
      </section>
    </>
  );
}

function CommentItem({
  comment,
  postId,
  depth,
}: {
  comment: CommentNode;
  postId: number;
  depth: number;
}) {
  const me = useAuthStore((s) => s.user);
  const [showReply, setShowReply] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const toggle = useToggleCommentReaction(postId);
  const del = useDeleteComment(postId);
  const isAuthor = me?.id === comment.authorId;

  return (
    <li
      className="rounded-[var(--radius-app)] border bg-surface p-3 shadow-sm"
      style={{ marginLeft: depth * 20 }}
    >
      <div className="flex items-center gap-2">
        <span
          className={`grid size-7 place-items-center rounded-full text-xs font-semibold ${
            comment.authorId === null
              ? "bg-neutral-100 text-muted"
              : tintFor(comment.authorId)
          }`}
        >
          {initialOf(comment.authorNickname)}
        </span>
        <span className="text-xs">
          <span className="font-medium">{comment.authorNickname}</span>
          <span className="text-muted"> · {formatDate(comment.createdAt)}</span>
        </span>
        {isAuthor && (
          <button
            onClick={() => confirm("삭제할까요?") && del.mutate(comment.id)}
            className="ml-auto text-[11px] text-muted hover:text-red-700"
          >
            삭제
          </button>
        )}
      </div>
      <p className="mt-2 whitespace-pre-wrap text-sm">{comment.content}</p>

      <div className="mt-2 flex flex-wrap items-center gap-1.5">
        {comment.reactions.map((r) => (
          <button
            key={r.emoji}
            onClick={() => toggle.mutate({ commentId: comment.id, emoji: r.emoji })}
            className={`rounded-full border px-2 py-0.5 text-xs ${
              r.myReaction
                ? "border-primary bg-primary/10 text-primary"
                : "bg-surface hover:border-primary/40"
            }`}
          >
            {r.emoji} {r.count}
          </button>
        ))}
        <button
          onClick={() => setShowEmojiPicker((v) => !v)}
          className="rounded-full border px-2 py-0.5 text-xs text-muted hover:text-foreground"
        >
          +
        </button>
        {showEmojiPicker && (
          <div className="flex gap-1">
            {QUICK_EMOJIS.map((e) => (
              <button
                key={e}
                onClick={() => {
                  toggle.mutate({ commentId: comment.id, emoji: e });
                  setShowEmojiPicker(false);
                }}
                className="rounded-full border bg-surface px-1.5 py-0.5 text-sm hover:border-primary/40"
              >
                {e}
              </button>
            ))}
          </div>
        )}
        {depth < 3 && (
          <button
            onClick={() => setShowReply((v) => !v)}
            className="text-[11px] text-muted hover:text-foreground"
          >
            답글
          </button>
        )}
        <ReportButton className="ml-auto" targetType="COMMENT" targetId={comment.id} tiny />
      </div>

      {showReply && (
        <div className="mt-3">
          <CommentComposer
            postId={postId}
            parentCommentId={comment.id}
            onDone={() => setShowReply(false)}
            small
          />
        </div>
      )}

      {comment.replies.length > 0 && (
        <ul className="mt-3 flex flex-col gap-3">
          {comment.replies.map((r) => (
            <CommentItem key={r.id} comment={r} postId={postId} depth={depth + 1} />
          ))}
        </ul>
      )}
    </li>
  );
}

function CommentComposer({
  postId,
  parentCommentId,
  onDone,
  small,
}: {
  postId: number;
  parentCommentId?: number;
  onDone?: () => void;
  small?: boolean;
}) {
  const [content, setContent] = useState("");
  const add = useAddComment(postId);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;
    add.mutate(
      { content: content.trim(), parentCommentId: parentCommentId ?? null },
      {
        onSuccess: () => {
          setContent("");
          onDone?.();
        },
      },
    );
  };

  return (
    <form onSubmit={submit} className="flex gap-2">
      <textarea
        required
        rows={small ? 2 : 3}
        maxLength={2000}
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder={parentCommentId ? "답글을 입력하세요" : "댓글을 입력하세요"}
        className="flex-1 rounded-lg border bg-surface px-3 py-2 text-sm focus:border-primary focus:outline-none"
      />
      <button
        type="submit"
        disabled={add.isPending || !content.trim()}
        className="self-end rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm hover:scale-[1.02] active:scale-95 disabled:opacity-50"
      >
        {parentCommentId ? "답글" : "등록"}
      </button>
    </form>
  );
}

function ReportButton({
  targetType,
  targetId,
  className,
  tiny,
}: {
  targetType: "POST" | "COMMENT";
  targetId: number;
  className?: string;
  tiny?: boolean;
}) {
  const report = useReport();
  const submit = () => {
    const reason = window.prompt("신고 사유를 입력해주세요.");
    if (!reason) return;
    report.mutate({ targetType, targetId, reason });
  };
  return (
    <button
      onClick={submit}
      disabled={report.isPending || report.isSuccess}
      className={`${
        tiny ? "text-[11px]" : "text-xs"
      } text-muted hover:text-red-700 disabled:opacity-40 ${className ?? ""}`}
    >
      {report.isSuccess ? "신고됨" : "신고"}
    </button>
  );
}
