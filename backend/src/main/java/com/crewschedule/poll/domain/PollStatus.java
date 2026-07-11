package com.crewschedule.poll.domain;

/** 투표 상태. OPEN에서만 투표 가능, CLOSED가 되면 winnerCandidateId가 확정된다. */
public enum PollStatus {
    OPEN,
    CLOSED
}
