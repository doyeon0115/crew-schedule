package com.crewschedule.common.web;

/**
 * 모든 REST 응답의 공통 래퍼. {@code { code, message, data }} 형태로 반환한다.
 */
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    public static <T> ApiResponse<T> of(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
