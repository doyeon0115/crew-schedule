package com.crewschedule.admin.controller;

import com.crewschedule.admin.dto.AdminDtos.AdminReport;
import com.crewschedule.admin.dto.AdminDtos.ReportRequest;
import com.crewschedule.admin.service.ReportService;
import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 일반 유저가 컨텐츠/유저를 신고할 때 사용. 처리 대시보드는 관리자 전용({@code /api/admin/reports}). */
@Tag(name = "Report", description = "부적절 컨텐츠/유저 신고")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 발생")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminReport> report(
            @CurrentUserId Long userId, @Valid @RequestBody ReportRequest request) {
        return ApiResponse.ok(reportService.report(userId, request));
    }
}
