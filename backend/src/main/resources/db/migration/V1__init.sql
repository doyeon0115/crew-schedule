-- V1: 핵심 도메인 초기 스키마
-- 사용자 / 크루(그룹) / 주간 스케줄 / 약속(RSVP)

-- ============ 사용자 ============
CREATE TABLE users (
    id                BIGSERIAL PRIMARY KEY,
    email             VARCHAR(255) NOT NULL,
    password          VARCHAR(255),              -- 소셜 로그인 사용자는 NULL
    nickname          VARCHAR(50)  NOT NULL,
    profile_image_url VARCHAR(500),
    role              VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

-- ============ 크루(그룹) ============
CREATE TABLE crews (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    invite_code VARCHAR(12) NOT NULL,             -- 초대 링크용 고유 코드
    owner_id    BIGINT      NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    CONSTRAINT uq_crews_invite_code UNIQUE (invite_code)
);

CREATE TABLE crew_members (
    id         BIGSERIAL PRIMARY KEY,
    crew_id    BIGINT      NOT NULL REFERENCES crews (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    CONSTRAINT uq_crew_members UNIQUE (crew_id, user_id),
    CONSTRAINT ck_crew_members_role CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE INDEX idx_crew_members_user ON crew_members (user_id);

-- ============ 주간 스케줄 ============
-- 사용자 단위 요일별 근무/휴무. 소속된 모든 크루에 공유된다.
CREATE TABLE weekly_slots (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day_of_week VARCHAR(9)  NOT NULL,
    is_off      BOOLEAN     NOT NULL DEFAULT FALSE,
    start_time  TIME,
    end_time    TIME,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    CONSTRAINT uq_weekly_slots UNIQUE (user_id, day_of_week),
    CONSTRAINT ck_weekly_slots_day CHECK (day_of_week IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT ck_weekly_slots_time CHECK (
        (is_off AND start_time IS NULL AND end_time IS NULL)
        OR (NOT is_off AND start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
    )
);

-- 특정 날짜 예외 일정(특별 근무/휴가 등). 주간 스케줄보다 우선한다.
CREATE TABLE schedule_exceptions (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exception_date DATE         NOT NULL,
    is_off         BOOLEAN      NOT NULL,
    start_time     TIME,
    end_time       TIME,
    memo           VARCHAR(200),
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uq_schedule_exceptions UNIQUE (user_id, exception_date),
    CONSTRAINT ck_schedule_exceptions_time CHECK (
        (is_off AND start_time IS NULL AND end_time IS NULL)
        OR (NOT is_off AND start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
    )
);

-- ============ 약속 ============
CREATE TABLE meetups (
    id         BIGSERIAL PRIMARY KEY,
    crew_id    BIGINT       NOT NULL REFERENCES crews (id) ON DELETE CASCADE,
    creator_id BIGINT       NOT NULL REFERENCES users (id),
    title      VARCHAR(100) NOT NULL,
    meet_date  DATE         NOT NULL,
    start_time TIME         NOT NULL,
    location   VARCHAR(200),
    memo       VARCHAR(500),
    status     VARCHAR(20)  NOT NULL DEFAULT 'PROPOSED',
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT ck_meetups_status CHECK (status IN ('PROPOSED', 'CONFIRMED', 'CANCELED'))
);

CREATE INDEX idx_meetups_crew_date ON meetups (crew_id, meet_date);

CREATE TABLE meetup_participants (
    id           BIGSERIAL PRIMARY KEY,
    meetup_id    BIGINT      NOT NULL REFERENCES meetups (id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rsvp         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    responded_at TIMESTAMP,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,
    CONSTRAINT uq_meetup_participants UNIQUE (meetup_id, user_id),
    CONSTRAINT ck_meetup_participants_rsvp CHECK (rsvp IN ('PENDING', 'ATTEND', 'MAYBE', 'ABSENT'))
);

CREATE INDEX idx_meetup_participants_user ON meetup_participants (user_id);
