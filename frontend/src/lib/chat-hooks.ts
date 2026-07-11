"use client";

import { useQuery } from "@tanstack/react-query";
import { Client, type IMessage } from "@stomp/stompjs";
import { useEffect, useRef, useState } from "react";
import { apiRequest } from "./api";
import { useAuthStore } from "./auth-store";
import type { ChatHistory, ChatMessage } from "./types";

/** 기본 WS URL — 개발은 백엔드 직결(Next rewrites는 WS 프록시 못 함). 프로덕션은 env로. */
const WS_URL =
  process.env.NEXT_PUBLIC_WS_URL ??
  (typeof window !== "undefined"
    ? window.location.protocol === "https:"
      ? `wss://${window.location.host}/ws`
      : "ws://localhost:8080/ws"
    : "ws://localhost:8080/ws");

export function useChatHistory(crewId: number | null) {
  return useQuery({
    queryKey: ["chat", crewId, "history"],
    queryFn: () =>
      apiRequest<ChatHistory>(`/api/crews/${crewId}/chat/messages?size=50`),
    enabled: crewId !== null,
    staleTime: 10_000,
  });
}

type Status = "idle" | "connecting" | "connected" | "closed";

/**
 * STOMP 연결·구독·전송을 관리하는 훅.
 * - CONNECT 프레임에 Bearer 토큰 첨부
 * - /topic/crews/{crewId} 구독
 * - send로 /app/crews/{crewId}/chat 발행
 * - 초기 히스토리는 상위 hook의 useChatHistory에서 로드하고, 이 훅은 실시간 append만 담당
 */
export function useChatSocket(crewId: number | null) {
  const accessToken = useAuthStore((s) => s.accessToken);
  const clientRef = useRef<Client | null>(null);
  const [status, setStatus] = useState<Status>("idle");
  const [liveMessages, setLiveMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    if (!crewId || !accessToken) return;
    setLiveMessages([]);

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setStatus("connected");
        client.subscribe(`/topic/crews/${crewId}`, (frame: IMessage) => {
          try {
            const msg = JSON.parse(frame.body) as ChatMessage;
            setLiveMessages((prev) => [...prev, msg]);
          } catch {
            // 무시 — 잘못된 페이로드
          }
        });
      },
      onWebSocketClose: () => setStatus("closed"),
      onStompError: () => setStatus("closed"),
    });
    setStatus("connecting");
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [crewId, accessToken]);

  const send = (content: string) => {
    const client = clientRef.current;
    if (!client || !client.connected || !crewId) return false;
    client.publish({
      destination: `/app/crews/${crewId}/chat`,
      body: JSON.stringify({ content }),
    });
    return true;
  };

  return { status, liveMessages, send };
}
