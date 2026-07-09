package com.crewschedule.common.exception;

import lombok.Getter;

/** 도메인/서비스 규칙 위반 시 던지는 공통 예외. {@link GlobalExceptionHandler}가 HTTP 응답으로 변환한다. */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
