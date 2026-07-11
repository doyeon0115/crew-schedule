package com.crewschedule.admin.domain;

import com.crewschedule.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부적절 컨텐츠·유저 신고.
 * 대상은 POST/COMMENT/CHAT_MESSAGE/USER 중 하나이며 (target_type, target_id)로 식별.
 * 관리자가 조치(정지/숨김 등)를 취한 뒤 RESOLVED로, 오신고면 DISMISSED로 마킹.
 */
@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    private Long handledBy;

    private LocalDateTime handledAt;

    @Column(length = 500)
    private String adminMemo;

    @Builder
    private Report(Long reporterId, ReportTargetType targetType, Long targetId, String reason) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.status = ReportStatus.PENDING;
    }

    public void resolve(Long adminId, String memo) {
        this.status = ReportStatus.RESOLVED;
        this.handledBy = adminId;
        this.handledAt = LocalDateTime.now();
        this.adminMemo = memo;
    }

    public void dismiss(Long adminId, String memo) {
        this.status = ReportStatus.DISMISSED;
        this.handledBy = adminId;
        this.handledAt = LocalDateTime.now();
        this.adminMemo = memo;
    }
}
