package com.crewschedule.user.domain;

/** 가입 경로. LOCAL은 이메일/비밀번호, 나머지는 OAuth2 소셜 로그인. */
public enum AuthProvider {
    LOCAL,
    KAKAO,
    GOOGLE
}
