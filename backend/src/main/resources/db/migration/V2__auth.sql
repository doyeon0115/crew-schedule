-- V2: 인증(Phase 1) - 소셜 로그인 컬럼 + 리프레시 토큰 저장소

-- ============ 사용자 - 소셜 로그인 컬럼 ============
-- provider: 가입 경로. LOCAL(이메일/비번) | KAKAO | GOOGLE
-- provider_id: 소셜 로그인 시 provider가 부여한 외부 유저 ID. LOCAL은 NULL.
ALTER TABLE users
    ADD COLUMN provider    VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_id VARCHAR(100);

ALTER TABLE users
    ADD CONSTRAINT ck_users_provider CHECK (provider IN ('LOCAL', 'KAKAO', 'GOOGLE'));

-- 소셜 유저는 (provider, provider_id)로 유일해야 함. LOCAL 유저는 provider_id NULL이므로 제약 밖.
CREATE UNIQUE INDEX uq_users_provider ON users (provider, provider_id)
    WHERE provider_id IS NOT NULL;

-- ============ 리프레시 토큰 ============
-- access 토큰은 무상태(JWT 서명 검증)로 처리하고, refresh만 서버가 보관해서 강제 로그아웃을 가능케 함.
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,       -- 원본 토큰이 아닌 해시(SHA-256)를 저장
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);
