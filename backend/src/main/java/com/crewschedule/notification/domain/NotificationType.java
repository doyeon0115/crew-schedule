package com.crewschedule.notification.domain;

/** 알림 카테고리. payload 스키마는 타입마다 다르며 서비스 계층에서 해석한다. */
public enum NotificationType {
    /** 크루에 새 약속이 제안됨. */
    MEETUP_PROPOSED,
    /** 내가 만든 약속에 누군가 참여함 (선착순). */
    MEETUP_JOINED,
    /** 참여 중인 약속이 확정됨. */
    MEETUP_CONFIRMED,
    /** 새 날짜 투표가 열림. */
    POLL_CREATED,
    /** 날짜 투표가 마감됨. */
    POLL_CLOSED,
    /** 크루 게시판에 새 글이 올라옴. */
    POST_CREATED,
    /** 내가 쓴 글에 댓글이 달림. */
    COMMENT_CREATED,
    /** 내가 쓴 댓글에 대댓글이 달림. */
    REPLY_CREATED
}
