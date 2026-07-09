package com.crewschedule.schedule.domain;

import com.crewschedule.common.entity.BaseTimeEntity;
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
import jakarta.persistence.UniqueConstraint;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 요일별 반복 스케줄(근무/휴무). 사용자당 요일별 1건이며,
 * 소속된 모든 크루의 가용시간 계산에 공유된다.
 */
@Getter
@Entity
@Table(
        name = "weekly_slots",
        uniqueConstraints = @UniqueConstraint(name = "uq_weekly_slots", columnNames = {"user_id", "day_of_week"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklySlot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private boolean isOff;

    private LocalTime startTime;

    private LocalTime endTime;

    @Builder
    private WeeklySlot(User user, DayOfWeek dayOfWeek, boolean isOff, LocalTime startTime, LocalTime endTime) {
        validateTimes(isOff, startTime, endTime);
        this.user = user;
        this.dayOfWeek = dayOfWeek;
        this.isOff = isOff;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** 휴무로 변경한다. 근무 시간은 비운다. */
    public void markOff() {
        this.isOff = true;
        this.startTime = null;
        this.endTime = null;
    }

    /** 근무일로 변경하고 근무 시간을 설정한다. */
    public void updateWorkHours(LocalTime startTime, LocalTime endTime) {
        validateTimes(false, startTime, endTime);
        this.isOff = false;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    private static void validateTimes(boolean isOff, LocalTime startTime, LocalTime endTime) {
        if (isOff) {
            if (startTime != null || endTime != null) {
                throw new IllegalArgumentException("휴무일에는 근무 시간을 설정할 수 없습니다.");
            }
            return;
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("근무일에는 시작/종료 시간이 모두 필요합니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("근무 시작 시간은 종료 시간보다 빨라야 합니다.");
        }
    }
}
