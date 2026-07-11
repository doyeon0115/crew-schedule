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
import jakarta.persistence.Version;
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

    /** 선착순 정원. null이면 무제한(초대형 약속). */
    private Integer capacity;

    /** 현재 참여자 수 O(1) 카운터 겸 낙관적 락 타깃. */
    @Column(nullable = false)
    private int currentParticipants;

    /** 낙관적 락(Phase 4)용. Phase 4 이전 데이터는 default 0. */
    @Version
    private Long version;

    @Builder
    private Meetup(
            Crew crew,
            User creator,
            String title,
            LocalDate meetDate,
            LocalTime startTime,
            String location,
            String memo,
            Integer capacity) {
        this.crew = crew;
        this.creator = creator;
        this.title = title;
        this.meetDate = meetDate;
        this.startTime = startTime;
        this.location = location;
        this.memo = memo;
        this.status = MeetupStatus.PROPOSED;
        this.capacity = capacity;
    }

    public boolean hasCapacityLimit() {
        return capacity != null;
    }

    /** 정원 초과 여부. 선착순 join 로직에서 사용. */
    public boolean isFull() {
        return hasCapacityLimit() && currentParticipants >= capacity;
    }

    /**
     * 참여자 카운터를 1 증가. capacity 초과면 예외.
     * <p>낙관적 락/비관적 락 전략에서 이 메서드로 참여자 슬롯을 예약한다.
     * Redis 원자 연산 전략에서는 Redis가 이미 슬롯을 확보했으므로 검사를 스킵할 수 있게 별도 메서드 제공.
     */
    public void reserveSlot() {
        if (isFull()) {
            throw new IllegalStateException("정원 초과");
        }
        this.currentParticipants++;
    }

    /** Redis 원자 연산 등 외부 게이팅으로 이미 슬롯이 확보된 경우에 호출. */
    public void incrementParticipantsWithoutCheck() {
        this.currentParticipants++;
    }

    /** 참여자 이탈 시. 카운터 감소. */
    public void releaseSlot() {
        if (currentParticipants > 0) {
            this.currentParticipants--;
        }
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
