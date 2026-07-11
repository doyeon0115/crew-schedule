"use client";

import { ChatRoom } from "@/components/ChatRoom";
import { useMyCrews } from "@/lib/schedule-hooks";

export default function ChatPage() {
  const crews = useMyCrews();
  const activeCrew = crews.data?.[0] ?? null;

  if (crews.isLoading) {
    return <p className="text-sm text-muted">불러오는 중...</p>;
  }
  if (!activeCrew) {
    return (
      <p className="text-sm text-muted">
        크루가 있어야 채팅을 시작할 수 있어요. 홈에서 먼저 크루를 만드세요.
      </p>
    );
  }

  return (
    <>
      <div className="mb-5">
        <h1 className="text-xl font-bold tracking-tight sm:text-2xl">
          채팅 · {activeCrew.name}
        </h1>
        <p className="mt-1 text-sm text-muted">
          크루원들과 실시간으로 소통해 보세요.
        </p>
      </div>
      <ChatRoom crewId={activeCrew.id} />
    </>
  );
}
