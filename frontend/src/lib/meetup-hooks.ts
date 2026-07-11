"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./api";
import type { CreateMeetupInput, Meetup, Rsvp } from "./types";

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

export function useJoinMeetup(crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (meetupId: number) =>
      apiRequest<Meetup>(`/api/meetups/${meetupId}/join`, { method: "POST" }),
    onSuccess: () => {
      if (crewId !== null) {
        qc.invalidateQueries({ queryKey: ["crews", crewId, "meetups"] });
      }
    },
  });
}

export function useRespondMeetup(crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ meetupId, rsvp }: { meetupId: number; rsvp: Rsvp }) =>
      apiRequest<Meetup>(`/api/meetups/${meetupId}/rsvp`, {
        method: "POST",
        body: { rsvp },
      }),
    onSuccess: () => {
      if (crewId !== null) {
        qc.invalidateQueries({ queryKey: ["crews", crewId, "meetups"] });
      }
    },
  });
}

export function useConfirmMeetup(crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (meetupId: number) =>
      apiRequest<Meetup>(`/api/meetups/${meetupId}/confirm`, { method: "POST" }),
    onSuccess: () => {
      if (crewId !== null) {
        qc.invalidateQueries({ queryKey: ["crews", crewId, "meetups"] });
      }
    },
  });
}

export function useCancelMeetup(crewId: number | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (meetupId: number) =>
      apiRequest<Meetup>(`/api/meetups/${meetupId}/cancel`, { method: "POST" }),
    onSuccess: () => {
      if (crewId !== null) {
        qc.invalidateQueries({ queryKey: ["crews", crewId, "meetups"] });
      }
    },
  });
}
