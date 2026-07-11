#!/usr/bin/env bash
# Phase 4 k6 부하 테스트용 시드 데이터 준비 스크립트.
#
# - 창시자 1명 + 참가자 1000명을 회원가입시켜 토큰 수집
# - 창시자가 크루 만들고 참가자 전원을 초대
# - 창시자가 정원 있는 meetup 생성
# - 참가자 토큰을 tokens.json으로 저장, meetupId를 stdout에 출력
#
# jq 필요. 사용: ./seed.sh [ACCOUNTS=1000] [CAPACITY=8]

set -euo pipefail

HOST="${HOST:-http://localhost:8080}"
ACCOUNTS="${1:-1000}"
CAPACITY="${2:-8}"
OUT="${OUT:-tokens.json}"

signup() {
  local email="$1" nickname="$2"
  curl -s -X POST "$HOST/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"password12\",\"nickname\":\"$nickname\"}" \
  | jq -r '.data.accessToken'
}

echo "signing up creator..." >&2
CREATOR_TOKEN=$(signup "creator-$(date +%s%N)@k6.local" "creator")

echo "creating crew..." >&2
CREW_ID=$(curl -s -X POST "$HOST/api/crews" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $CREATOR_TOKEN" \
  -d '{"name":"k6crew"}' | jq -r '.data.id')
INVITE=$(curl -s "$HOST/api/crews" -H "Authorization: Bearer $CREATOR_TOKEN" \
  | jq -r --arg id "$CREW_ID" '.data[] | select(.id == ($id|tonumber)) | .inviteCode')

echo "signing up + joining $ACCOUNTS contenders..." >&2
: > "$OUT.tmp"
echo '[' > "$OUT.tmp"
for i in $(seq 1 "$ACCOUNTS"); do
  TOKEN=$(signup "u${i}-$(date +%s%N)@k6.local" "u$i")
  curl -s -X POST "$HOST/api/crews/join" \
    -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
    -d "{\"inviteCode\":\"$INVITE\"}" > /dev/null
  if [ "$i" -eq 1 ]; then
    printf '"%s"' "$TOKEN" >> "$OUT.tmp"
  else
    printf ',"%s"' "$TOKEN" >> "$OUT.tmp"
  fi
  if [ $((i % 100)) -eq 0 ]; then
    echo "  $i/$ACCOUNTS" >&2
  fi
done
echo ']' >> "$OUT.tmp"
mv "$OUT.tmp" "$OUT"

echo "creating flash meetup with capacity=$CAPACITY..." >&2
MEETUP_ID=$(curl -s -X POST "$HOST/api/crews/$CREW_ID/meetups" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $CREATOR_TOKEN" \
  -d "{\"title\":\"번개\",\"meetDate\":\"2026-12-31\",\"startTime\":\"19:00:00\",\"capacity\":$CAPACITY}" \
  | jq -r '.data.id')

echo "seed done." >&2
echo "MEETUP_ID=$MEETUP_ID"
echo "TOKENS_FILE=$OUT"
