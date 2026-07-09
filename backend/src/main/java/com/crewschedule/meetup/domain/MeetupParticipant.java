package com.crewschedule.meetup.domain;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 약속에 초대된 참여자와 RSVP 응답. */
@Getter
@Entity
@Table(
        name = "meetup_participants",
        uniqueConstraints =
                @UniqueConstraint(name = "uq_meetup_participants", columnNames = {"meetup_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetupParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meetup_id")
    private Meetup meetup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rsvp rsvp;

    private LocalDateTime respondedAt;

    @Builder
    private MeetupParticipant(Meetup meetup, User user) {
        this.meetup = meetup;
        this.user = user;
        this.rsvp = Rsvp.PENDING;
    }

    /** 참석 여부를 응답한다. 응답은 마감 전까지 변경할 수 있다. */
    public void respond(Rsvp rsvp) {
        if (rsvp == Rsvp.PENDING) {
            throw new IllegalArgumentException("PENDING으로는 응답할 수 없습니다.");
        }
        this.rsvp = rsvp;
        this.respondedAt = LocalDateTime.now();
    }
}
