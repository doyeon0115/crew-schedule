"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./api";
import type {
  AdminReportList,
  AdminUser,
  AdminUserList,
  DashboardStats,
} from "./types";

export function useAdminStats() {
  return useQuery({
    queryKey: ["admin", "stats"],
    queryFn: () => apiRequest<DashboardStats>("/api/admin/stats"),
    staleTime: 15_000,
  });
}

export function useAdminUsers(query: string) {
  return useQuery({
    queryKey: ["admin", "users", query],
    queryFn: () => {
      const suffix = query ? `?query=${encodeURIComponent(query)}` : "";
      return apiRequest<AdminUserList>(`/api/admin/users${suffix}`);
    },
    staleTime: 10_000,
  });
}

function useUserMutation(action: "suspend" | "reactivate" | "promote" | "demote") {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userId: number) =>
      apiRequest<AdminUser>(`/api/admin/users/${userId}/${action}`, { method: "POST" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "users"] });
      qc.invalidateQueries({ queryKey: ["admin", "stats"] });
    },
  });
}

export const useSuspendUser = () => useUserMutation("suspend");
export const useReactivateUser = () => useUserMutation("reactivate");
export const usePromoteUser = () => useUserMutation("promote");
export const useDemoteUser = () => useUserMutation("demote");

export function usePendingReports() {
  return useQuery({
    queryKey: ["admin", "reports", "pending"],
    queryFn: () => apiRequest<AdminReportList>("/api/admin/reports/pending"),
    staleTime: 10_000,
  });
}

function useReportMutation(action: "resolve" | "dismiss") {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ reportId, memo }: { reportId: number; memo: string }) =>
      apiRequest(`/api/admin/reports/${reportId}/${action}`, {
        method: "POST",
        body: { memo },
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "reports"] });
      qc.invalidateQueries({ queryKey: ["admin", "stats"] });
    },
  });
}

export const useResolveReport = () => useReportMutation("resolve");
export const useDismissReport = () => useReportMutation("dismiss");

/** 신고 대상 컨텐츠 숨김/복구 (관리자 액션에서 파생) */
export function useHidePost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (postId: number) =>
      apiRequest(`/api/admin/posts/${postId}/hide`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin"] }),
  });
}

export function useHideComment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (commentId: number) =>
      apiRequest(`/api/admin/comments/${commentId}/hide`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin"] }),
  });
}
