package com.crewschedule.meetup.domain;

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
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 크루 내 약속 제안. 제안(PROPOSED) 후 확정(CONFIRMED) 또는 취소(CANCELED)된다. */
@Getter
@Entity
@Table(name = "meetups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meetup extends BaseTimeEntity {

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

    @Column(nullable = false)
    private LocalDate meetDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(length = 200)
    private String location;

    @Column(length = 500)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetupStatus status;

    @Builder
    private Meetup(
            Crew crew,
            User creator,
            String title,
            LocalDate meetDate,
            LocalTime startTime,
            String location,
            String memo) {
        this.crew = crew;
        this.creator = creator;
        this.title = title;
        this.meetDate = meetDate;
        this.startTime = startTime;
        this.location = location;
        this.memo = memo;
        this.status = MeetupStatus.PROPOSED;
    }

    public void confirm() {
        requireProposed();
        this.status = MeetupStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.status == MeetupStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 약속입니다.");
        }
        this.status = MeetupStatus.CANCELED;
    }

    public void updateDetails(String title, LocalDate meetDate, LocalTime startTime, String location, String memo) {
        requireProposed();
        this.title = title;
        this.meetDate = meetDate;
        this.startTime = startTime;
        this.location = location;
        this.memo = memo;
    }

    private void requireProposed() {
        if (this.status != MeetupStatus.PROPOSED) {
            throw new IllegalStateException("제안 상태의 약속만 변경할 수 있습니다.");
        }
    }
}
