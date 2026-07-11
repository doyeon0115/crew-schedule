package com.crewschedule.auth.oauth;

import com.crewschedule.user.domain.AuthProvider;

/** OAuth provider에서 조회한 정규화된 사용자 정보. */
public record OAuthUserInfo(
        AuthProvider provider,
        String providerId,
        String email,
        String nickname,
        String profileImageUrl) {
}
