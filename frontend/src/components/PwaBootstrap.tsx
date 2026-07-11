"use client";

import { useEffect, useState } from "react";

/**
 * 서비스 워커 등록 + 브라우저의 설치 프롬프트를 잡아 사용자에게 안내 배너 노출.
 * layout 근처에서 한 번만 마운트하면 됨.
 */
type BeforeInstallPromptEvent = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed" }>;
};

export function PwaBootstrap() {
  const [installEvent, setInstallEvent] = useState<BeforeInstallPromptEvent | null>(null);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    if (process.env.NODE_ENV !== "production") return; // dev에서는 sw 등록 X
    if (typeof window === "undefined" || !("serviceWorker" in navigator)) return;
    navigator.serviceWorker.register("/sw.js").catch(() => undefined);
  }, []);

  useEffect(() => {
    const handler = (e: Event) => {
      e.preventDefault();
      setInstallEvent(e as BeforeInstallPromptEvent);
    };
    window.addEventListener("beforeinstallprompt", handler);
    return () => window.removeEventListener("beforeinstallprompt", handler);
  }, []);

  if (!installEvent || dismissed) return null;

  return (
    <div className="fixed inset-x-0 bottom-4 z-30 mx-auto max-w-md px-4">
      <div className="flex items-center gap-3 rounded-full border bg-surface px-4 py-2 shadow-lg">
        <span className="text-lg">📅</span>
        <p className="flex-1 text-xs">홈 화면에 앱으로 설치할 수 있어요.</p>
        <button
          onClick={async () => {
            await installEvent.prompt();
            const { outcome } = await installEvent.userChoice;
            if (outcome === "accepted" || outcome === "dismissed") {
              setInstallEvent(null);
            }
          }}
          className="rounded-full bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground"
        >
          설치
        </button>
        <button
          onClick={() => setDismissed(true)}
          className="text-xs text-muted hover:text-foreground"
        >
          닫기
        </button>
      </div>
    </div>
  );
}
