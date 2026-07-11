package com.crewschedule.admin.dto;

import com.crewschedule.admin.domain.Report;
import com.crewschedule.admin.domain.ReportStatus;
import com.crewschedule.admin.domain.ReportTargetType;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.domain.UserRole;
import com.crewschedule.user.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {}

    public record DashboardStats(
            long totalUsers,
            long activeUsers,
            long suspendedUsers,
            long adminUsers,
            long totalCrews,
            long totalMeetups,
            long pendingReports) {}

    public record AdminUser(
            Long id,
            String email,
            String nickname,
            UserRole role,
            UserStatus status,
            LocalDateTime createdAt) {

        public static AdminUser from(User u) {
            return new AdminUser(
                    u.getId(), u.getEmail(), u.getNickname(), u.getRole(), u.getStatus(), u.getCreatedAt());
        }
    }

    public record UserList(List<AdminUser> users) {}

    public record ReportRequest(
            @NotNull ReportTargetType targetType,
            @NotNull Long targetId,
            @NotBlank @Size(max = 500) String reason) {}

    public record AdminReport(
            Long id,
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            String reason,
            ReportStatus status,
            Long handledBy,
            LocalDateTime handledAt,
            String adminMemo,
            LocalDateTime createdAt) {

        public static AdminReport from(Report r) {
            return new AdminReport(
                    r.getId(),
                    r.getReporterId(),
                    r.getTargetType(),
                    r.getTargetId(),
                    r.getReason(),
                    r.getStatus(),
                    r.getHandledBy(),
                    r.getHandledAt(),
                    r.getAdminMemo(),
                    r.getCreatedAt());
        }
    }

    public record ReportList(List<AdminReport> reports, long pendingCount) {}

    public record HandleReportRequest(@Size(max = 500) String memo) {}
}
