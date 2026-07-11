"use client";

import { useState } from "react";
import {
  useDismissReport,
  useHideComment,
  useHidePost,
  usePendingReports,
  useResolveReport,
  useSuspendUser,
} from "@/lib/admin-hooks";
import type { AdminReport, ReportTargetType } from "@/lib/types";

const TARGET_LABEL: Record<ReportTargetType, string> = {
  POST: "게시글",
  COMMENT: "댓글",
  CHAT_MESSAGE: "채팅 메시지",
  USER: "유저",
};

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString("ko-KR", { dateStyle: "short", timeStyle: "short" });
}

export default function AdminReportsPage() {
  const { data, isLoading } = usePendingReports();

  if (isLoading) return <p className="text-sm text-muted">불러오는 중...</p>;

  const reports = data?.reports ?? [];

  return (
    <section className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted">
          미처리 신고 <span className="font-semibold">{data?.pendingCount ?? 0}</span>건
        </p>
      </div>

      {reports.length === 0 ? (
        <div className="rounded-[var(--radius-app)] border bg-surface p-8 text-center text-sm text-muted">
          미처리 신고가 없어요.
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {reports.map((r) => (
            <ReportCard key={r.id} report={r} />
          ))}
        </ul>
      )}
    </section>
  );
}

function ReportCard({ report }: { report: AdminReport }) {
  const [memo, setMemo] = useState("");
  const resolve = useResolveReport();
  const dismiss = useDismissReport();
  const hidePost = useHidePost();
  const hideComment = useHideComment();
  const suspend = useSuspendUser();

  const applyContentAction = () => {
    if (report.targetType === "POST") {
      hidePost.mutate(report.targetId);
    } else if (report.targetType === "COMMENT") {
      hideComment.mutate(report.targetId);
    } else if (report.targetType === "USER") {
      suspend.mutate(report.targetId);
    }
  };
  const contentActionLabel =
    report.targetType === "USER"
      ? "대상 유저 정지"
      : report.targetType === "CHAT_MESSAGE"
        ? null
        : "대상 컨텐츠 숨김";

  return (
    <li className="overflow-hidden rounded-[var(--radius-app)] border bg-surface shadow-sm">
      <div className="flex items-center justify-between gap-3 border-b bg-red-50/40 px-4 py-2">
        <div className="flex items-center gap-2">
          <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-bold text-red-700">
            {TARGET_LABEL[report.targetType]}
          </span>
          <span className="text-xs text-muted">
            #{report.targetId} · 신고자 {report.reporterId}
          </span>
        </div>
        <span className="text-[10px] text-muted">{formatDate(report.createdAt)}</span>
      </div>

      <div className="flex flex-col gap-2 px-4 py-3">
        <p className="text-sm">{report.reason}</p>
      </div>

      <div className="flex flex-col gap-2 border-t bg-background/50 px-4 py-3 sm:flex-row sm:items-center">
        <input
          type="text"
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
          placeholder="처리 메모 (선택)"
          className="flex-1 rounded-lg border bg-surface px-3 py-1.5 text-sm focus:border-primary focus:outline-none"
        />
        <div className="flex flex-wrap gap-2 sm:justify-end">
          {contentActionLabel && (
            <button
              onClick={applyContentAction}
              className="rounded-full border border-red-300 bg-red-50 px-3 py-1.5 text-xs font-medium text-red-700 hover:bg-red-100"
            >
              {contentActionLabel}
            </button>
          )}
          <button
            onClick={() => resolve.mutate({ reportId: report.id, memo })}
            disabled={resolve.isPending}
            className="rounded-full bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground hover:scale-[1.02] active:scale-95 disabled:opacity-50"
          >
            처리 완료
          </button>
          <button
            onClick={() => dismiss.mutate({ reportId: report.id, memo })}
            disabled={dismiss.isPending}
            className="rounded-full border px-3 py-1.5 text-xs font-medium text-muted hover:text-foreground disabled:opacity-50"
          >
            기각
          </button>
        </div>
      </div>
    </li>
  );
}
