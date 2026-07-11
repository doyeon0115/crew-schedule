package com.crewschedule.admin.service;

import com.crewschedule.admin.domain.Report;
import com.crewschedule.admin.domain.ReportStatus;
import com.crewschedule.admin.dto.AdminDtos.AdminReport;
import com.crewschedule.admin.dto.AdminDtos.HandleReportRequest;
import com.crewschedule.admin.dto.AdminDtos.ReportList;
import com.crewschedule.admin.dto.AdminDtos.ReportRequest;
import com.crewschedule.admin.repository.ReportRepository;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 신고 발생(일반 유저) + 관리자 대시보드 조회/처리. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportRepository reportRepository;

    /** 로그인 유저가 신고 발생. */
    @Transactional
    public AdminReport report(Long reporterId, ReportRequest request) {
        Report saved = reportRepository.save(Report.builder()
                .reporterId(reporterId)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .build());
        return AdminReport.from(saved);
    }

    /** 관리자: 대기 중인 신고 목록. */
    public ReportList listPending(Integer size) {
        int pageSize = Math.min(size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        List<Report> rows = reportRepository.findAllByStatusOrderByCreatedAtDesc(
                ReportStatus.PENDING, PageRequest.of(0, pageSize));
        long pending = reportRepository.countByStatus(ReportStatus.PENDING);
        return new ReportList(rows.stream().map(AdminReport::from).toList(), pending);
    }

    @Transactional
    public AdminReport resolve(Long adminId, Long reportId, HandleReportRequest request) {
        Report r = load(reportId);
        ensurePending(r);
        r.resolve(adminId, request.memo());
        return AdminReport.from(r);
    }

    @Transactional
    public AdminReport dismiss(Long adminId, Long reportId, HandleReportRequest request) {
        Report r = load(reportId);
        ensurePending(r);
        r.dismiss(adminId, request.memo());
        return AdminReport.from(r);
    }

    private Report load(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
    }

    private void ensurePending(Report r) {
        if (r.getStatus() != ReportStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_HANDLED);
        }
    }
}
