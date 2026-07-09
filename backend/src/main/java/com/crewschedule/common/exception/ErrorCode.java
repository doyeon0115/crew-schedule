package com.crewschedule.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 비즈니스 오류 코드. API 응답의 {@code code} 필드로 그대로 내려간다. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // 크루
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "크루를 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."),
    ALREADY_CREW_MEMBER(HttpStatus.CONFLICT, "이미 가입한 크루입니다."),
    NOT_CREW_MEMBER(HttpStatus.FORBIDDEN, "크루 멤버가 아닙니다."),

    // 약속
    MEETUP_NOT_FOUND(HttpStatus.NOT_FOUND, "약속을 찾을 수 없습니다."),
    NOT_MEETUP_PARTICIPANT(HttpStatus.FORBIDDEN, "약속 참여 대상이 아닙니다."),
    NOT_MEETUP_CREATOR(HttpStatus.FORBIDDEN, "약속 생성자만 할 수 있는 작업입니다.");

    private final HttpStatus status;
    private final String message;
}
