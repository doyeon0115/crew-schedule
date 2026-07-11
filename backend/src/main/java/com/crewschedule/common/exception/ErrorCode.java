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

    // 인증
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "소셜 로그인 처리에 실패했습니다."),
    SOCIAL_USER_PASSWORD_LOGIN(HttpStatus.BAD_REQUEST, "소셜 로그인 계정은 이메일 로그인을 사용할 수 없습니다."),

    // 크루
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "크루를 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."),
    ALREADY_CREW_MEMBER(HttpStatus.CONFLICT, "이미 가입한 크루입니다."),
    NOT_CREW_MEMBER(HttpStatus.FORBIDDEN, "크루 멤버가 아닙니다."),

    // 약속
    MEETUP_NOT_FOUND(HttpStatus.NOT_FOUND, "약속을 찾을 수 없습니다."),
    NOT_MEETUP_PARTICIPANT(HttpStatus.FORBIDDEN, "약속 참여 대상이 아닙니다."),
    NOT_MEETUP_CREATOR(HttpStatus.FORBIDDEN, "약속 생성자만 할 수 있는 작업입니다."),
    MEETUP_FULL(HttpStatus.CONFLICT, "정원이 마감되었습니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여한 약속입니다."),
    MEETUP_NOT_JOINABLE(HttpStatus.BAD_REQUEST, "선착순으로 참여 가능한 약속이 아닙니다."),
    JOIN_LOCK_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "잠시 후 다시 시도해 주세요."),

    // 투표
    POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."),
    POLL_CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "투표 후보를 찾을 수 없습니다."),
    POLL_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 마감된 투표입니다."),
    POLL_HAS_NO_CANDIDATES(HttpStatus.BAD_REQUEST, "후보 날짜가 없으면 마감할 수 없습니다."),
    POLL_HAS_NO_VOTES(HttpStatus.BAD_REQUEST, "득표한 후보가 없어 마감할 수 없습니다."),
    POLL_NEEDS_CANDIDATES(HttpStatus.BAD_REQUEST, "후보 날짜는 하나 이상 있어야 합니다."),
    NOT_POLL_CREATOR(HttpStatus.FORBIDDEN, "투표 생성자만 할 수 있는 작업입니다."),
    DUPLICATE_POLL_VOTE(HttpStatus.CONFLICT, "이미 투표한 후보입니다.");

    private final HttpStatus status;
    private final String message;
}
