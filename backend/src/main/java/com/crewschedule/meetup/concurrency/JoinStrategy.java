package com.crewschedule.meetup.concurrency;

/**
 * 선착순 참여(join) 시 정원 검사 + 슬롯 예약 + 참여자 저장을 원자적으로 처리하는 전략.
 *
 * <p>구현체는 반드시 다음을 보장해야 한다:
 * <ol>
 *   <li>정원 초과 시 {@code MEETUP_FULL} 예외를 던진다.</li>
 *   <li>동시 요청 N건 중 정원만큼만 성공한다(오버부킹 금지).</li>
 *   <li>이미 참여한 유저는 {@code ALREADY_JOINED}를 던진다.</li>
 * </ol>
 */
public interface JoinStrategy {

    JoinStrategyType type();

    /** 참여 처리. 성공 시 저장된 MeetupParticipant의 id 반환. */
    Long join(Long meetupId, Long userId);
}
