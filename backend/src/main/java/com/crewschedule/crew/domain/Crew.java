package com.crewschedule.crew.domain;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 친구 그룹(크루). 고유 초대 코드로 멤버를 초대한다. */
@Getter
@Entity
@Table(name = "crews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crew extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 12)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Builder
    private Crew(String name, String inviteCode, User owner) {
        this.name = name;
        this.inviteCode = inviteCode;
        this.owner = owner;
    }

    public void rename(String name) {
        this.name = name;
    }
}
