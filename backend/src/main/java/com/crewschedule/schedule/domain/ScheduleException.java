package com.crewschedule.schedule.domain;

import com.crewschedule.common.entity.BaseTimeEntity;
import com.crewschedule.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특정 날짜의 예외 일정(특별 근무·휴가 등).
 * 같은 날짜의 주간 스케줄({@link WeeklySlot})보다 우선한다.
 */
@Getter
@Entity
@Table(
        name = "schedule_exceptions",
        uniqueConstraints =
                @UniqueConstraint(name = "uq_schedule_exceptions", columnNames = {"user_id", "exception_date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleException extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "exception_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean isOff;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(length = 200)
    private String memo;

    @Builder
    private ScheduleException(
            User user, LocalDate date, boolean isOff, LocalTime startTime, LocalTime endTime, String memo) {
        if (!isOff && (startTime == null || endTime == null || !startTime.isBefore(endTime))) {
            throw new IllegalArgumentException("근무 예외에는 올바른 시작/종료 시간이 필요합니다.");
        }
        if (isOff && (startTime != null || endTime != null)) {
            throw new IllegalArgumentException("휴무 예외에는 근무 시간을 설정할 수 없습니다.");
        }
        this.user = user;
        this.date = date;
        this.isOff = isOff;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
    }
}
