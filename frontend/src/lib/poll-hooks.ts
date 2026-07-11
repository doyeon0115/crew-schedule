"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./api";
import type { CreatePollInput, Poll } from "./types";

export function useCrewPolls(crewId: number | null) {
  return useQuery({
    queryKey: ["crews", crewId, "polls"],
    queryFn: () => apiRequest<Poll[]>(`/api/crews/${crewId}/polls`),
    enabled: crewId !== null,
  });
}

export function usePoll(pollId: number | null) {
  return useQuery({
    queryKey: ["polls", pollId],
    queryFn: () => apiRequest<Poll>(`/api/polls/${pollId}`),
    enabled: pollId !== null,
  });
}

export function useCreatePoll(crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreatePollInput) =>
      apiRequest<Poll>(`/api/crews/${crewId}/polls`, {
        method: "POST",
        body: input,
      }),
    onSuccess: () => {
      if (crewId !== null) {
        qc.invalidateQueries({ queryKey: ["crews", crewId, "polls"] });
      }
    },
  });
}

function invalidateAll(qc: ReturnType<typeof useQueryClient>, pollId: number, crewId: number | null) {
  qc.invalidateQueries({ queryKey: ["polls", pollId] });
  if (crewId !== null) {
    qc.invalidateQueries({ queryKey: ["crews", crewId, "polls"] });
  }
}

export function useVote(pollId: number, crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (candidateId: number) =>
      apiRequest<Poll>(`/api/polls/${pollId}/candidates/${candidateId}/vote`, {
        method: "POST",
      }),
    onSuccess: () => invalidateAll(qc, pollId, crewId),
  });
}

export function useUnvote(pollId: number, crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (candidateId: number) =>
      apiRequest<Poll>(`/api/polls/${pollId}/candidates/${candidateId}/vote`, {
        method: "DELETE",
      }),
    onSuccess: () => invalidateAll(qc, pollId, crewId),
  });
}

export function useClosePoll(pollId: number, crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () =>
      apiRequest<Poll>(`/api/polls/${pollId}/close`, { method: "POST" }),
    onSuccess: () => invalidateAll(qc, pollId, crewId),
  });
}
