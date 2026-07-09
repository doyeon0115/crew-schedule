package com.crewschedule.crew.domain;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 크루 소속 멤버. 크루당 사용자 1명은 한 번만 가입할 수 있다. */
@Getter
@Entity
@Table(
        name = "crew_members",
        uniqueConstraints = @UniqueConstraint(name = "uq_crew_members", columnNames = {"crew_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id")
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrewRole role;

    @Builder
    private CrewMember(Crew crew, User user, CrewRole role) {
        this.crew = crew;
        this.user = user;
        this.role = role != null ? role : CrewRole.MEMBER;
    }
}
