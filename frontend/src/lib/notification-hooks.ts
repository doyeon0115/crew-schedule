"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Client } from "@stomp/stompjs";
import { useEffect } from "react";
import { apiRequest } from "./api";
import { useAuthStore } from "./auth-store";
import type { NotificationItem, NotificationPage } from "./types";

const WS_URL =
  process.env.NEXT_PUBLIC_WS_URL ??
  (typeof window !== "undefined"
    ? window.location.protocol === "https:"
      ? `wss://${window.location.host}/ws`
      : "ws://localhost:8080/ws"
    : "ws://localhost:8080/ws");

export function useNotifications() {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: () => apiRequest<NotificationPage>("/api/notifications?size=30"),
    staleTime: 15_000,
  });
}

export function useMarkAllRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () =>
      apiRequest<{ updated: number }>("/api/notifications/read-all", { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["notifications"] }),
  });
}

/**
 * 개인 큐 /user/queue/notifications 구독. 새 알림이 오면 캐시에 prepend + unreadCount++.
 * 앱 전역에 하나만 있으면 되므로 layout 근처에서 마운트하도록 설계.
 */
export function useNotificationSocket() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const qc = useQueryClient();

  useEffect(() => {
    if (!accessToken) return;

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe("/user/queue/notifications", (frame) => {
          try {
            const item = JSON.parse(frame.body) as NotificationItem;
            qc.setQueryData<NotificationPage | undefined>(["notifications"], (prev) => {
              if (!prev) return { notifications: [item], nextBeforeId: null, unreadCount: 1 };
              // id 중복 방지
              if (prev.notifications.some((n) => n.id === item.id)) return prev;
              return {
                notifications: [item, ...prev.notifications],
                nextBeforeId: prev.nextBeforeId,
                unreadCount: prev.unreadCount + 1,
              };
            });
          } catch {
            // ignore
          }
        });
      },
    });
    client.activate();
    return () => {
      client.deactivate();
    };
  }, [accessToken, qc]);
}
