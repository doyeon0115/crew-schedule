package com.crewschedule.auth.oauth;

import com.crewschedule.user.domain.AuthProvider;

/** 각 OAuth provider의 API 어댑터. 인가 코드로 토큰을 교환하고 유저 정보를 정규화해서 돌려준다. */
public interface OAuthClient {

    AuthProvider provider();

    OAuthUserInfo fetchUser(String authorizationCode);
}
