package com.crewschedule.poll.domain;

import com.crewschedule.common.entity.BaseTimeEntity;
import com.crewschedule.crew.domain.Crew;
import com.crewschedule.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 여러 후보 날짜 중 하나를 투표로 확정하는 그룹 투표. */
@Getter
@Entity
@Table(name = "date_polls")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DatePoll extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id")
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollStatus status;

    /** CLOSED 상태에서만 값이 있음. 순환 참조 방지 위해 FK 없이 id만 보관. */
    private Long winnerCandidateId;

    private LocalDateTime closedAt;

    @Builder
    private DatePoll(Crew crew, User creator, String title) {
        this.crew = crew;
        this.creator = creator;
        this.title = title;
        this.status = PollStatus.OPEN;
    }

    public void close(Long winnerCandidateId) {
        if (status == PollStatus.CLOSED) {
            throw new IllegalStateException("이미 마감된 투표입니다.");
        }
        this.status = PollStatus.CLOSED;
        this.winnerCandidateId = winnerCandidateId;
        this.closedAt = LocalDateTime.now();
    }

    public boolean isClosed() {
        return status == PollStatus.CLOSED;
    }
}
