-- V7: 게시판(Phase 6B) — 크루별 글 + 댓글(대댓글은 자기참조) + 이모지 반응

CREATE TABLE posts (
    id         BIGSERIAL PRIMARY KEY,
    crew_id    BIGINT       NOT NULL REFERENCES crews (id) ON DELETE CASCADE,
    author_id  BIGINT       NOT NULL REFERENCES users (id),
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);
CREATE INDEX idx_posts_crew_created ON posts (crew_id, created_at DESC);

-- comments는 자기참조로 대댓글까지 수용. parent_comment_id가 null이면 최상위 댓글.
CREATE TABLE comments (
    id                BIGSERIAL PRIMARY KEY,
    post_id           BIGINT      NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    author_id         BIGINT      NOT NULL REFERENCES users (id),
    parent_comment_id BIGINT      REFERENCES comments (id) ON DELETE CASCADE,
    content           VARCHAR(2000) NOT NULL,
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL
);
CREATE INDEX idx_comments_post ON comments (post_id, id);
CREATE INDEX idx_comments_parent ON comments (parent_comment_id) WHERE parent_comment_id IS NOT NULL;

-- 게시글·댓글 공용 이모지 반응. post_id / comment_id 중 하나만 세팅(XOR).
CREATE TABLE reactions (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT REFERENCES posts (id) ON DELETE CASCADE,
    comment_id BIGINT REFERENCES comments (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    emoji      VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    CONSTRAINT ck_reactions_target CHECK (
        (post_id IS NOT NULL AND comment_id IS NULL)
        OR (post_id IS NULL AND comment_id IS NOT NULL)
    )
);
CREATE UNIQUE INDEX uq_reactions_post ON reactions (post_id, user_id, emoji) WHERE post_id IS NOT NULL;
CREATE UNIQUE INDEX uq_reactions_comment ON reactions (comment_id, user_id, emoji) WHERE comment_id IS NOT NULL;
CREATE INDEX idx_reactions_post ON reactions (post_id) WHERE post_id IS NOT NULL;
CREATE INDEX idx_reactions_comment ON reactions (comment_id) WHERE comment_id IS NOT NULL;
