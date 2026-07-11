"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./api";
import type {
  CreateCommentInput,
  CreatePostInput,
  PostDetail,
  PostListResponse,
  PostSummary,
  ReactionSummary,
} from "./types";

export function useCrewPosts(crewId: number | null) {
  return useQuery({
    queryKey: ["crews", crewId, "posts"],
    queryFn: () => apiRequest<PostListResponse>(`/api/crews/${crewId}/posts`),
    enabled: crewId !== null,
  });
}

export function usePost(postId: number | null) {
  return useQuery({
    queryKey: ["posts", postId],
    queryFn: () => apiRequest<PostDetail>(`/api/posts/${postId}`),
    enabled: postId !== null,
  });
}

export function useCreatePost(crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreatePostInput) =>
      apiRequest<PostSummary>(`/api/crews/${crewId}/posts`, {
        method: "POST",
        body: input,
      }),
    onSuccess: () => {
      if (crewId !== null) {
        qc.invalidateQueries({ queryKey: ["crews", crewId, "posts"] });
      }
    },
  });
}

export function useDeletePost(crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (postId: number) =>
      apiRequest<void>(`/api/posts/${postId}`, { method: "DELETE" }),
    onSuccess: () => {
      if (crewId !== null) {
        qc.invalidateQueries({ queryKey: ["crews", crewId, "posts"] });
      }
    },
  });
}

export function useAddComment(postId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateCommentInput) =>
      apiRequest(`/api/posts/${postId}/comments`, {
        method: "POST",
        body: input,
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["posts", postId] }),
  });
}

export function useDeleteComment(postId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (commentId: number) =>
      apiRequest<void>(`/api/comments/${commentId}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["posts", postId] }),
  });
}

export function useTogglePostReaction(postId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (emoji: string) =>
      apiRequest<ReactionSummary[]>(`/api/posts/${postId}/reactions`, {
        method: "POST",
        body: { emoji },
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["posts", postId] }),
  });
}

export function useToggleCommentReaction(postId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ commentId, emoji }: { commentId: number; emoji: string }) =>
      apiRequest<ReactionSummary[]>(`/api/comments/${commentId}/reactions`, {
        method: "POST",
        body: { emoji },
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["posts", postId] }),
  });
}

/** 유저용 신고 훅. 관리자 콘솔 훅과 별도. */
export function useReport() {
  return useMutation({
    mutationFn: ({
      targetType,
      targetId,
      reason,
    }: {
      targetType: "POST" | "COMMENT" | "CHAT_MESSAGE" | "USER";
      targetId: number;
      reason: string;
    }) =>
      apiRequest(`/api/reports`, {
        method: "POST",
        body: { targetType, targetId, reason },
      }),
  });
}
