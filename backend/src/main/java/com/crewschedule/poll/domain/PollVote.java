package com.crewschedule.poll.domain;

import com.crewschedule.common.entity.BaseTimeEntity;
import com.crewschedule.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 후보에 대한 투표 한 표. (candidate, user) UNIQUE. */
@Getter
@Entity
@Table(name = "poll_votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id")
    private PollCandidate candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    private PollVote(PollCandidate candidate, User user) {
        this.candidate = candidate;
        this.user = user;
    }
}
