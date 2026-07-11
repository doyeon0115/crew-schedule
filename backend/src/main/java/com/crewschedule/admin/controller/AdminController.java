package com.crewschedule.admin.controller;

import com.crewschedule.admin.dto.AdminDtos.AdminReport;
import com.crewschedule.admin.dto.AdminDtos.AdminUser;
import com.crewschedule.admin.dto.AdminDtos.DashboardStats;
import com.crewschedule.admin.dto.AdminDtos.HandleReportRequest;
import com.crewschedule.admin.dto.AdminDtos.ReportList;
import com.crewschedule.admin.dto.AdminDtos.UserList;
import com.crewschedule.admin.service.AdminService;
import com.crewschedule.admin.service.ReportService;
import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "관리자 전용 - 통계 · 유저 · 컨텐츠 · 신고 (ROLE_ADMIN)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    // ============ 통계 ============

    @Operation(summary = "대시보드 통계")
    @GetMapping("/stats")
    public ApiResponse<DashboardStats> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    // ============ 유저 관리 ============

    @Operation(summary = "유저 목록/검색")
    @GetMapping("/users")
    public ApiResponse<UserList> users(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(adminService.searchUsers(query, size));
    }

    @Operation(summary = "유저 정지 (활성 refresh 토큰 폐기)")
    @PostMapping("/users/{userId}/suspend")
    public ApiResponse<AdminUser> suspend(
            @CurrentUserId Long adminId, @PathVariable Long userId) {
        return ApiResponse.ok(adminService.suspendUser(adminId, userId));
    }

    @Operation(summary = "유저 정지 해제")
    @PostMapping("/users/{userId}/reactivate")
    public ApiResponse<AdminUser> reactivate(@PathVariable Long userId) {
        return ApiResponse.ok(adminService.reactivateUser(userId));
    }

    @Operation(summary = "관리자 권한 부여")
    @PostMapping("/users/{userId}/promote")
    public ApiResponse<AdminUser> promote(@PathVariable Long userId) {
        return ApiResponse.ok(adminService.promoteToAdmin(userId));
    }

    @Operation(summary = "관리자 권한 강등")
    @PostMapping("/users/{userId}/demote")
    public ApiResponse<AdminUser> demote(
            @CurrentUserId Long adminId, @PathVariable Long userId) {
        return ApiResponse.ok(adminService.demoteToUser(adminId, userId));
    }

    // ============ 컨텐츠 제재 ============

    @Operation(summary = "게시글 숨김")
    @PostMapping("/posts/{postId}/hide")
    public ApiResponse<Void> hidePost(@PathVariable Long postId) {
        adminService.hidePost(postId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "게시글 복구")
    @PostMapping("/posts/{postId}/restore")
    public ApiResponse<Void> restorePost(@PathVariable Long postId) {
        adminService.restorePost(postId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "댓글 숨김")
    @PostMapping("/comments/{commentId}/hide")
    public ApiResponse<Void> hideComment(@PathVariable Long commentId) {
        adminService.hideComment(commentId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "댓글 복구")
    @PostMapping("/comments/{commentId}/restore")
    public ApiResponse<Void> restoreComment(@PathVariable Long commentId) {
        adminService.restoreComment(commentId);
        return ApiResponse.ok(null);
    }

    // ============ 신고 대시보드 ============

    @Operation(summary = "대기 중인 신고 목록")
    @GetMapping("/reports/pending")
    public ApiResponse<ReportList> pendingReports(@RequestParam(required = false) Integer size) {
        return ApiResponse.ok(reportService.listPending(size));
    }

    @Operation(summary = "신고 처리(조치 완료)")
    @PostMapping("/reports/{reportId}/resolve")
    public ApiResponse<AdminReport> resolveReport(
            @CurrentUserId Long adminId,
            @PathVariable Long reportId,
            @Valid @RequestBody HandleReportRequest request) {
        return ApiResponse.ok(reportService.resolve(adminId, reportId, request));
    }

    @Operation(summary = "신고 기각(오신고)")
    @PostMapping("/reports/{reportId}/dismiss")
    public ApiResponse<AdminReport> dismissReport(
            @CurrentUserId Long adminId,
            @PathVariable Long reportId,
            @Valid @RequestBody HandleReportRequest request) {
        return ApiResponse.ok(reportService.dismiss(adminId, reportId, request));
    }
}
