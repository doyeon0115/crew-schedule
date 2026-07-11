// Phase 4 부하 테스트 — 선착순 참여 API에 대해 각 락 전략의 처리량·정합성 비교
//
// 사용:
//   1) 백엔드를 특정 전략으로 기동
//      JOIN_STRATEGY=PESSIMISTIC ./gradlew bootRun
//   2) 시드 스크립트로 유저 N명·크루·정원 있는 meetup 생성 후 이 스크립트에 주입
//   3) k6 실행
//      k6 run -e HOST=http://localhost:8080 -e MEETUP_ID=1 -e TOKENS_FILE=tokens.json infra/k6/join.js
//
// tokens.json 포맷: ["<jwt1>", "<jwt2>", ...]
// (각 유저의 access token. 시드 스크립트에서 signup + login 응답에서 뽑아 저장.)

import http from "k6/http";
import { check, fail } from "k6";
import { SharedArray } from "k6/data";

const HOST = __ENV.HOST || "http://localhost:8080";
const MEETUP_ID = __ENV.MEETUP_ID || fail("MEETUP_ID env required");
const TOKENS_FILE = __ENV.TOKENS_FILE || fail("TOKENS_FILE env required");

const tokens = new SharedArray("tokens", () => JSON.parse(open(TOKENS_FILE)));

export const options = {
  // 1000 VU가 동시에 몰려 정원(예: 8) 초과 요청 시뮬레이션
  scenarios: {
    burst: {
      executor: "per-vu-iterations",
      vus: 1000,
      iterations: 1,
      maxDuration: "30s",
    },
  },
  thresholds: {
    // 정원 이내로 성공/실패 비율만 확인 — 처리량은 별도 metric으로.
    "http_req_failed{expected_response:true}": ["rate<0.99"], // 99% 이상 예상 실패 = 정원 초과 401/409
  },
};

export default function () {
  // VU마다 다른 토큰. VU 수 > 토큰 수면 wrap around.
  const token = tokens[(__VU - 1) % tokens.length];
  const res = http.post(
    `${HOST}/api/meetups/${MEETUP_ID}/join`,
    null,
    {
      headers: { Authorization: `Bearer ${token}` },
      tags: { endpoint: "join" },
    },
  );
  check(res, {
    "200 or 409/503": (r) => [200, 409, 503].includes(r.status),
  });
}
