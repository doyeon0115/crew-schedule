package com.crewschedule.board.domain;

/** 게시글/댓글 등 UGC의 노출 상태. HIDDEN이면 목록·상세 조회에서 제외된다. */
public enum ContentStatus {
    ACTIVE,
    HIDDEN
}
