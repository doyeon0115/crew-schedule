package com.crewschedule.poll.domain;

import com.crewschedule.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 투표 후보 날짜. 시간은 미정(null) 허용. */
@Getter
@Entity
@Table(name = "poll_candidates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollCandidate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id")
    private DatePoll poll;

    @Column(nullable = false)
    private LocalDate candidateDate;

    private LocalTime startTime;

    @Builder
    private PollCandidate(DatePoll poll, LocalDate candidateDate, LocalTime startTime) {
        this.poll = poll;
        this.candidateDate = candidateDate;
        this.startTime = startTime;
    }
}
