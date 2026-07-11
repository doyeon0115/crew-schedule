-- V4: 선착순 정원(Phase 4)
-- capacity가 null이면 정원 무제한(기존 초대형 약속과 호환).
-- current_participants는 O(1) 카운터 겸 낙관적 락(@Version) 타깃.

ALTER TABLE meetups
    ADD COLUMN capacity             INTEGER,
    ADD COLUMN current_participants INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN version              BIGINT  NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_meetups_capacity CHECK (capacity IS NULL OR capacity > 0),
    ADD CONSTRAINT ck_meetups_participants CHECK (
        current_participants >= 0
        AND (capacity IS NULL OR current_participants <= capacity)
    );
