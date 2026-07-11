// Crew Schedule 최소 서비스 워커
// - Next.js 정적 자원(SWR 스타일) 캐시로 오프라인 시 앱 셸이 뜨도록
// - API 요청은 항상 네트워크 우선 (인증 정보가 걸린 데이터가 캐시되면 안 됨)
// PWA 인스톨과 브라우저 뒤로가기 캐시 정도만 커버하는 최소 구성.

const CACHE = "crew-schedule-v1";
const APP_SHELL = ["/", "/icon.svg", "/manifest.webmanifest"];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(APP_SHELL).catch(() => undefined)),
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))),
    ),
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  // API·인증은 캐시 우회
  if (url.pathname.startsWith("/api") || url.pathname.startsWith("/ws")) return;
  if (event.request.method !== "GET") return;

  event.respondWith(
    (async () => {
      try {
        const response = await fetch(event.request);
        if (response.ok && response.type === "basic") {
          const clone = response.clone();
          caches.open(CACHE).then((cache) => cache.put(event.request, clone));
        }
        return response;
      } catch {
        const cached = await caches.match(event.request);
        return cached || Response.error();
      }
    })(),
  );
});
