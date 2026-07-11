package com.crewschedule.admin.service;

import com.crewschedule.admin.dto.AdminDtos.AdminUser;
import com.crewschedule.admin.dto.AdminDtos.DashboardStats;
import com.crewschedule.admin.dto.AdminDtos.UserList;
import com.crewschedule.auth.repository.RefreshTokenRepository;
import com.crewschedule.board.domain.Comment;
import com.crewschedule.board.domain.Post;
import com.crewschedule.board.repository.CommentRepository;
import com.crewschedule.board.repository.PostRepository;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.repository.CrewRepository;
import com.crewschedule.meetup.repository.MeetupRepository;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.domain.UserStatus;
import com.crewschedule.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 통계 · 유저 관리 · 컨텐츠 제재.
 * {@code /api/admin/**}는 SecurityConfig에서 ROLE_ADMIN으로 잠겨 있으므로 서비스 계층은 권한 재검증하지 않음.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final MeetupRepository meetupRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.crewschedule.admin.repository.ReportRepository reportRepository;

    public DashboardStats stats() {
        long total = userRepository.count();
        long suspended = userRepository.countByStatus(UserStatus.SUSPENDED);
        long active = total - suspended;
        long admins = userRepository.searchForAdmin(null, PageRequest.of(0, MAX_PAGE_SIZE))
                .stream()
                .filter(u -> u.getRole() == com.crewschedule.user.domain.UserRole.ADMIN)
                .count();
        return new DashboardStats(
                total,
                active,
                suspended,
                admins,
                crewRepository.count(),
                meetupRepository.count(),
                reportRepository.countByStatus(com.crewschedule.admin.domain.ReportStatus.PENDING));
    }

    public UserList searchUsers(String query, Integer size) {
        int pageSize = clampPageSize(size);
        List<User> rows = userRepository.searchForAdmin(query, PageRequest.of(0, pageSize));
        return new UserList(rows.stream().map(AdminUser::from).toList());
    }

    /** 유저 정지. 활성 refresh 토큰 전량 폐기해서 다음 요청부터 접근 못 하게 한다. */
    @Transactional
    public AdminUser suspendUser(Long adminId, Long targetUserId) {
        User target = load(targetUserId);
        if (target.getId().equals(adminId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "관리자 본인은 정지할 수 없습니다.");
        }
        target.suspend();
        refreshTokenRepository.deleteAllByUserId(targetUserId);
        return AdminUser.from(target);
    }

    @Transactional
    public AdminUser reactivateUser(Long targetUserId) {
        User target = load(targetUserId);
        target.reactivate();
        return AdminUser.from(target);
    }

    @Transactional
    public AdminUser promoteToAdmin(Long targetUserId) {
        User target = load(targetUserId);
        target.promoteToAdmin();
        return AdminUser.from(target);
    }

    @Transactional
    public AdminUser demoteToUser(Long adminId, Long targetUserId) {
        if (targetUserId.equals(adminId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "관리자 본인은 강등할 수 없습니다.");
        }
        User target = load(targetUserId);
        target.demoteToUser();
        return AdminUser.from(target);
    }

    // ======= 컨텐츠 제재 =======

    @Transactional
    public void hidePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        post.hide();
    }

    @Transactional
    public void restorePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        post.restore();
    }

    @Transactional
    public void hideComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        comment.hide();
    }

    @Transactional
    public void restoreComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        comment.restore();
    }

    private User load(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private int clampPageSize(Integer size) {
        int s = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
        return Math.min(s, MAX_PAGE_SIZE);
    }
}
