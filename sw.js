/* 서비스워커: 네트워크 우선 + 오프라인 폴백 (업데이트가 막히지 않게) */
const CACHE = "crew-cache-v1";

self.addEventListener("install", (e) => { self.skipWaiting(); });

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET") return;            // 쓰기 요청은 패스
  e.respondWith(
    fetch(req)                                 // 항상 네트워크 먼저(최신)
      .then((res) => {
        const copy = res.clone();
        caches.open(CACHE).then((c) => c.put(req, copy)).catch(() => {});
        return res;
      })
      .catch(() => caches.match(req))          // 오프라인이면 캐시
  );
});
