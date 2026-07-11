package com.crewschedule.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crewschedule.board.dto.BoardDtos.CommentResponse;
import com.crewschedule.board.dto.BoardDtos.CreateCommentRequest;
import com.crewschedule.board.dto.BoardDtos.CreatePostRequest;
import com.crewschedule.board.dto.BoardDtos.PostDetail;
import com.crewschedule.board.dto.BoardDtos.PostSummary;
import com.crewschedule.board.dto.BoardDtos.ReactionRequest;
import com.crewschedule.board.dto.BoardDtos.ReactionSummary;
import com.crewschedule.board.service.BoardService;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.dto.CrewDtos.CrewResponse;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
 * Phase 6B 게시판 서비스 검증.
 * 게시글 생성 → 댓글 → 대댓글 → 이모지 반응 토글의 전체 흐름과 권한 케이스.
 * (알림은 여기서 검증 안 함 — 6A 파이프라인 테스트에서 커버.)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class BoardServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository userRepository;
    @Autowired CrewService crewService;
    @Autowired BoardService boardService;

    Long aliceId;
    Long bobId;
    Long outsiderId;
    Long crewId;

    @BeforeEach
    void setUp() {
        aliceId = createUser("alice@t.local", "alice");
        bobId = createUser("bob@t.local", "bob");
        outsiderId = createUser("out@t.local", "out");
        CrewResponse crew = crewService.create(aliceId, "우리끼리");
        crewService.join(bobId, crew.inviteCode());
        crewId = crew.id();
    }

    @Test
    @DisplayName("작성 → 댓글 → 대댓글 → 상세 조회 시 트리 구조로 반환")
    void createPostThenCommentsBuildTree() {
        PostSummary post = boardService.createPost(aliceId, crewId,
                new CreatePostRequest("공지", "이번 주 모임 어때요?"));
        assertThat(post.commentCount()).isZero();

        CommentResponse top1 = boardService.addComment(bobId, post.id(),
                new CreateCommentRequest("좋아요", null));
        CommentResponse reply = boardService.addComment(aliceId, post.id(),
                new CreateCommentRequest("고마워요", top1.id()));
        CommentResponse top2 = boardService.addComment(bobId, post.id(),
                new CreateCommentRequest("근데 몇 시?", null));

        PostDetail detail = boardService.getPost(aliceId, post.id());
        assertThat(detail.comments()).hasSize(2);
        assertThat(detail.comments().get(0).id()).isEqualTo(top1.id());
        assertThat(detail.comments().get(0).replies()).hasSize(1);
        assertThat(detail.comments().get(0).replies().get(0).content()).isEqualTo("고마워요");
        assertThat(detail.comments().get(1).id()).isEqualTo(top2.id());
        assertThat(detail.comments().get(1).replies()).isEmpty();
        // reply의 parentCommentId가 세팅됨
        assertThat(detail.comments().get(0).replies().get(0).parentCommentId()).isEqualTo(top1.id());
    }

    @Test
    @DisplayName("이모지 반응은 토글 — 같은 이모지 두 번 누르면 사라지고 다시 누르면 생김")
    void reactionToggle() {
        PostSummary post = boardService.createPost(aliceId, crewId,
                new CreatePostRequest("반응 테스트", "이모지"));
        List<ReactionSummary> after1 = boardService.togglePostReaction(
                bobId, post.id(), new ReactionRequest("👍"));
        assertThat(after1).singleElement().satisfies(r -> {
            assertThat(r.emoji()).isEqualTo("👍");
            assertThat(r.count()).isEqualTo(1);
            assertThat(r.myReaction()).isTrue();
            assertThat(r.userIds()).containsExactly(bobId);
        });

        List<ReactionSummary> after2 = boardService.togglePostReaction(
                bobId, post.id(), new ReactionRequest("👍"));
        assertThat(after2).isEmpty();

        // 다른 유저가 다른 이모지
        boardService.togglePostReaction(aliceId, post.id(), new ReactionRequest("❤️"));
        boardService.togglePostReaction(bobId, post.id(), new ReactionRequest("❤️"));
        List<ReactionSummary> after3 = boardService.togglePostReaction(
                aliceId, post.id(), new ReactionRequest("👍"));
        assertThat(after3).hasSize(2);
        ReactionSummary heart = after3.stream().filter(r -> r.emoji().equals("❤️")).findFirst().orElseThrow();
        assertThat(heart.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("작성자만 수정·삭제할 수 있고, 크루 비멤버는 접근할 수 없다")
    void permissions() {
        PostSummary post = boardService.createPost(aliceId, crewId,
                new CreatePostRequest("보안", "권한 테스트"));

        assertThatThrownBy(() -> boardService.updatePost(bobId, post.id(),
                new com.crewschedule.board.dto.BoardDtos.UpdatePostRequest("훔치기", "노노")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);

        assertThatThrownBy(() -> boardService.getPost(outsiderId, post.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CREW_MEMBER);

        assertThatThrownBy(() -> boardService.deletePost(bobId, post.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_POST_AUTHOR);
    }

    @Test
    @DisplayName("다른 게시글 댓글을 부모로 지정할 수 없다")
    void crossPostReplyIsRejected() {
        PostSummary p1 = boardService.createPost(aliceId, crewId,
                new CreatePostRequest("게시글1", "본문"));
        PostSummary p2 = boardService.createPost(aliceId, crewId,
                new CreatePostRequest("게시글2", "본문"));
        CommentResponse c1 = boardService.addComment(bobId, p1.id(),
                new CreateCommentRequest("첫번째", null));

        assertThatThrownBy(() -> boardService.addComment(bobId, p2.id(),
                new CreateCommentRequest("잘못된 대댓글", c1.id())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_TARGET_MISMATCH);
    }

    private Long createUser(String email, String nickname) {
        return userRepository.save(User.builder().email(email).nickname(nickname).build()).getId();
    }
}
