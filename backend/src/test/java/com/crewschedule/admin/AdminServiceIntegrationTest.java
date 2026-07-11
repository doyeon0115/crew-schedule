package com.crewschedule.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crewschedule.admin.domain.ReportStatus;
import com.crewschedule.admin.domain.ReportTargetType;
import com.crewschedule.admin.dto.AdminDtos.AdminReport;
import com.crewschedule.admin.dto.AdminDtos.AdminUser;
import com.crewschedule.admin.dto.AdminDtos.DashboardStats;
import com.crewschedule.admin.dto.AdminDtos.HandleReportRequest;
import com.crewschedule.admin.dto.AdminDtos.ReportList;
import com.crewschedule.admin.dto.AdminDtos.ReportRequest;
import com.crewschedule.admin.service.AdminService;
import com.crewschedule.admin.service.ReportService;
import com.crewschedule.auth.dto.AuthDtos.LoginRequest;
import com.crewschedule.auth.dto.AuthDtos.SignupRequest;
import com.crewschedule.auth.repository.RefreshTokenRepository;
import com.crewschedule.auth.service.AuthService;
import com.crewschedule.board.dto.BoardDtos.CreatePostRequest;
import com.crewschedule.board.dto.BoardDtos.PostSummary;
import com.crewschedule.board.service.BoardService;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.dto.CrewDtos.CrewResponse;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.domain.UserRole;
import com.crewschedule.user.domain.UserStatus;
import com.crewschedule.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Phase 7 관리자 시스템 검증.
 * 통계 / 유저 정지 → 로그인 차단 → 해제 / 신고 접수 → 처리 / 게시글 숨김 → 조회 차단.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class AdminServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository userRepository;
    @Autowired AuthService authService;
    @Autowired CrewService crewService;
    @Autowired BoardService boardService;
    @Autowired AdminService adminService;
    @Autowired ReportService reportService;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("통계: 유저·크루·미처리 신고 카운트")
    void dashboardStats() {
        Long adminId = createUser("admin@t.local", "admin", UserRole.ADMIN);
        Long u1 = createUser("u1@t.local", "u1", UserRole.USER);
        createUser("u2@t.local", "u2", UserRole.USER);
        CrewResponse crew = crewService.create(u1, "크루A");
        reportService.report(u1, new ReportRequest(ReportTargetType.USER, adminId, "잘못 눌렀어요"));

        DashboardStats s = adminService.stats();
        assertThat(s.totalUsers()).isEqualTo(3);
        assertThat(s.suspendedUsers()).isZero();
        assertThat(s.totalCrews()).isEqualTo(1);
        assertThat(s.pendingReports()).isEqualTo(1);
        assertThat(crew.id()).isNotNull();
    }

    @Test
    @DisplayName("유저 정지 → 로그인 차단 + refresh 토큰 삭제, 해제 후 다시 로그인 가능")
    void suspendBlocksLoginAndPurgesRefresh() {
        Long adminId = createUser("admin@t.local", "admin", UserRole.ADMIN);
        // 실제 회원가입 흐름으로 refresh 토큰이 생기게 만든다
        authService.signup(new SignupRequest("bad@t.local", "password12", "bad"));
        User bad = userRepository.findByEmail("bad@t.local").orElseThrow();
        assertThat(refreshTokenRepository.count()).isPositive();

        AdminUser suspended = adminService.suspendUser(adminId, bad.getId());
        assertThat(suspended.status()).isEqualTo(UserStatus.SUSPENDED);
        // 정지 시 활성 refresh 토큰 폐기
        assertThat(refreshTokenRepository.count()).isZero();

        assertThatThrownBy(() -> authService.login(new LoginRequest("bad@t.local", "password12")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED);

        adminService.reactivateUser(bad.getId());
        // 해제 후에는 다시 로그인 가능
        assertThat(authService.login(new LoginRequest("bad@t.local", "password12")).user().id())
                .isEqualTo(bad.getId());
    }

    @Test
    @DisplayName("관리자 본인 정지·강등은 불가")
    void selfActionIsRejected() {
        Long adminId = createUser("admin@t.local", "admin", UserRole.ADMIN);
        assertThatThrownBy(() -> adminService.suspendUser(adminId, adminId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThatThrownBy(() -> adminService.demoteToUser(adminId, adminId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("게시글 숨김 처리 → 조회 시 not found, 복구 후 다시 조회 가능")
    void hiddenPostIsNotVisible() {
        Long aliceId = createUser("alice@t.local", "alice", UserRole.USER);
        CrewResponse crew = crewService.create(aliceId, "크루");
        PostSummary post = boardService.createPost(aliceId, crew.id(),
                new CreatePostRequest("문제글", "내용"));

        adminService.hidePost(post.id());
        assertThatThrownBy(() -> boardService.getPost(aliceId, post.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        adminService.restorePost(post.id());
        assertThat(boardService.getPost(aliceId, post.id()).title()).isEqualTo("문제글");
    }

    @Test
    @DisplayName("신고 접수 → 관리자 목록 조회 → resolve, 다시 처리 시 400")
    void reportLifecycle() {
        Long reporterId = createUser("rep@t.local", "reporter", UserRole.USER);
        Long adminId = createUser("admin@t.local", "admin", UserRole.ADMIN);

        AdminReport r = reportService.report(reporterId,
                new ReportRequest(ReportTargetType.POST, 999L, "부적절 컨텐츠"));
        assertThat(r.status()).isEqualTo(ReportStatus.PENDING);

        ReportList list = reportService.listPending(null);
        assertThat(list.pendingCount()).isEqualTo(1);
        assertThat(list.reports()).singleElement()
                .satisfies(x -> assertThat(x.id()).isEqualTo(r.id()));

        AdminReport resolved = reportService.resolve(adminId, r.id(), new HandleReportRequest("숨김 처리"));
        assertThat(resolved.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(resolved.handledBy()).isEqualTo(adminId);
        assertThat(resolved.adminMemo()).isEqualTo("숨김 처리");

        assertThatThrownBy(() -> reportService.resolve(adminId, r.id(), new HandleReportRequest(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_HANDLED);
    }

    private Long createUser(String email, String nickname, UserRole role) {
        return userRepository.save(
                User.builder().email(email).nickname(nickname).role(role).build()).getId();
    }
}
