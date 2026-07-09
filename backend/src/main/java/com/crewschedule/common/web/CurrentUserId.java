package com.crewschedule.common.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 현재 사용자 id를 주입한다.
 *
 * <p>TODO(Phase 3): 지금은 {@code X-User-Id} 헤더를 읽는 임시 방식이며,
 * JWT 인증 도입 시 SecurityContext의 principal에서 읽도록 교체한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
