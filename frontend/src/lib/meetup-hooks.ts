"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./api";
import type { CreateMeetupInput, Meetup } from "./types";

export function useCrewMeetups(crewId: number | null) {
  return useQuery({
    queryKey: ["crews", crewId, "meetups"],
    queryFn: () => apiRequest<Meetup[]>(`/api/crews/${crewId}/meetups`),
    enabled: crewId !== null,
  });
}

export function useProposeMeetup(crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateMeetupInput) =>
      apiRequest<Meetup>(`/api/crews/${crewId}/meetups`, {
        method: "POST",
        body: input,
      }),
    onSuccess: () => {
      if (crewId !== null) {
        qc.invalidateQueries({ queryKey: ["crews", crewId, "meetups"] });
      }
    },
  });
}
