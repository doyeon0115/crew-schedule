package com.crewschedule.auth.security;

import com.crewschedule.user.domain.UserRole;

/** SecurityContext에 들어가는 인증 principal. JWT의 sub·role 클레임에서 복원된다. */
public record AuthPrincipal(Long userId, UserRole role) {
}
