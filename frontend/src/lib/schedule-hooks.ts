"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./api";
import type {
  Crew,
  CrewScheduleBoard,
  DayRecommendation,
  SlotUpdate,
  WeeklySchedule,
} from "./types";

export function useMyCrews() {
  return useQuery({
    queryKey: ["crews", "mine"],
    queryFn: () => apiRequest<Crew[]>("/api/crews"),
  });
}

export function useCrewBoard(crewId: number | null) {
  return useQuery({
    queryKey: ["crews", crewId, "schedules"],
    queryFn: () =>
      apiRequest<CrewScheduleBoard>(`/api/crews/${crewId}/schedules`),
    enabled: crewId !== null,
  });
}

export function useRecommendations(crewId: number | null, startDate?: string) {
  return useQuery({
    queryKey: ["crews", crewId, "recommendations", startDate ?? "today"],
    queryFn: () => {
      const suffix = startDate ? `?startDate=${startDate}` : "";
      return apiRequest<DayRecommendation[]>(
        `/api/crews/${crewId}/recommendations${suffix}`,
      );
    },
    enabled: crewId !== null,
  });
}

export function useMySchedule() {
  return useQuery({
    queryKey: ["me", "schedule"],
    queryFn: () => apiRequest<WeeklySchedule>("/api/me/schedule"),
  });
}

export function useUpdateMySchedule(crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (slots: SlotUpdate[]) =>
      apiRequest<WeeklySchedule>("/api/me/schedule", {
        method: "PUT",
        body: slots,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["me", "schedule"] });
      if (crewId !== null) {
        qc.invalidateQueries({ queryKey: ["crews", crewId, "schedules"] });
        qc.invalidateQueries({ queryKey: ["crews", crewId, "recommendations"] });
      }
    },
  });
}

type CreateCrewInput = { name: string };
export function useCreateCrew() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateCrewInput) =>
      apiRequest<Crew>("/api/crews", { method: "POST", body: input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["crews", "mine"] }),
  });
}

type JoinCrewInput = { inviteCode: string };
export function useJoinCrew() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: JoinCrewInput) =>
      apiRequest<Crew>("/api/crews/join", { method: "POST", body: input }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["crews", "mine"] }),
  });
}
