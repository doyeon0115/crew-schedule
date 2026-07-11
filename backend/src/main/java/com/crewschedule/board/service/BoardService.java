package com.crewschedule.board.service;

import com.crewschedule.board.domain.Comment;
import com.crewschedule.board.domain.Post;
import com.crewschedule.board.domain.Reaction;
import com.crewschedule.board.dto.BoardDtos.CommentResponse;
import com.crewschedule.board.dto.BoardDtos.CreateCommentRequest;
import com.crewschedule.board.dto.BoardDtos.CreatePostRequest;
import com.crewschedule.board.dto.BoardDtos.PostDetail;
import com.crewschedule.board.dto.BoardDtos.PostListResponse;
import com.crewschedule.board.dto.BoardDtos.PostSummary;
import com.crewschedule.board.dto.BoardDtos.ReactionRequest;
import com.crewschedule.board.dto.BoardDtos.ReactionSummary;
import com.crewschedule.board.dto.BoardDtos.UpdateCommentRequest;
import com.crewschedule.board.dto.BoardDtos.UpdatePostRequest;
import com.crewschedule.board.repository.CommentRepository;
import com.crewschedule.board.repository.PostRepository;
import com.crewschedule.board.repository.ReactionRepository;
import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.domain.Crew;
import com.crewschedule.crew.service.CrewService;
import com.crewschedule.notification.domain.NotificationType;
import com.crewschedule.notification.service.NotificationDispatcher;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 크루 게시판 CRUD + 이모지 반응 토글.
 *
 * <p>게시글 목록은 필드 수를 줄이려고 요약 페이지 형태로 반환하고,
 * 상세 조회에서 댓글/대댓글 트리와 반응 그룹핑을 함께 로드한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final CrewService crewService;
    private final NotificationDispatcher notifier;

    // ============ Post ============

    @Transactional
    public PostSummary createPost(Long userId, Long crewId, CreatePostRequest request) {
        Crew crew = crewService.getCrewForMember(userId, crewId);
        User author = loadUser(userId);
        Post saved = postRepository.save(Post.builder()
                .crew(crew)
                .author(author)
                .title(request.title())
                .content(request.content())
                .build());
        notifier.dispatchToCrew(
                NotificationType.POST_CREATED,
                crewId,
                userId,
                Set.of(userId),
                Map.of("postId", saved.getId(), "title", saved.getTitle()));
        return PostSummary.from(saved, 0);
    }

    public PostListResponse listPosts(Long userId, Long crewId, Long beforeId, Integer size) {
        crewService.getCrewForMember(userId, crewId);
        int pageSize = clampPageSize(size);
        List<Post> rows = postRepository.findPage(crewId, beforeId, PageRequest.of(0, pageSize));
        Long nextBeforeId = rows.size() < pageSize ? null : rows.get(rows.size() - 1).getId();
        // 요약에는 댓글 수 카운트를 붙이지 않고 0으로. 상세에서 로드. (필요하면 별도 쿼리 최적화.)
        List<PostSummary> summaries = rows.stream().map(p -> PostSummary.from(p, 0)).toList();
        return new PostListResponse(summaries, nextBeforeId);
    }

    public PostDetail getPost(Long userId, Long postId) {
        Post post = loadPost(postId);
        crewService.getCrewForMember(userId, post.getCrew().getId());
        if (post.isHidden()) {
            // 숨김 처리된 게시글은 일반 유저에게 not found. 관리자는 admin 도구로 봐야 함.
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        List<Reaction> postReactions = reactionRepository.findAllByPostIdWithUser(postId);
        List<ReactionSummary> postReactionSummaries = summarizeReactions(postReactions, userId);

        List<Comment> allComments = commentRepository.findAllByPostIdWithAuthor(postId);
        Map<Long, List<Reaction>> reactionsByComment = allComments.isEmpty()
                ? Map.of()
                : reactionRepository
                        .findAllByCommentIdInWithUser(allComments.stream().map(Comment::getId).toList())
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(r -> r.getComment().getId()));
        List<CommentResponse> tree = buildCommentTree(allComments, reactionsByComment, userId);
        return PostDetail.of(post, postReactionSummaries, tree);
    }

    @Transactional
    public PostSummary updatePost(Long userId, Long postId, UpdatePostRequest request) {
        Post post = loadPost(postId);
        crewService.getCrewForMember(userId, post.getCrew().getId());
        if (!post.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POST_AUTHOR);
        }
        post.update(request.title(), request.content());
        return PostSummary.from(post, 0);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = loadPost(postId);
        crewService.getCrewForMember(userId, post.getCrew().getId());
        if (!post.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POST_AUTHOR);
        }
        postRepository.delete(post);
    }

    // ============ Comment ============

    @Transactional
    public CommentResponse addComment(Long userId, Long postId, CreateCommentRequest request) {
        Post post = loadPost(postId);
        crewService.getCrewForMember(userId, post.getCrew().getId());
        User author = loadUser(userId);

        Comment parent = null;
        if (request.parentCommentId() != null) {
            parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
            if (!parent.getPost().getId().equals(postId)) {
                throw new BusinessException(ErrorCode.COMMENT_TARGET_MISMATCH);
            }
        }
        Comment saved = commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .parent(parent)
                .content(request.content())
                .build());

        // 알림: 대댓글이면 부모 댓글 작성자에게, 최상위 댓글이면 게시글 작성자에게
        Long recipient = parent != null ? parent.getAuthor().getId() : post.getAuthor().getId();
        NotificationType type = parent != null
                ? NotificationType.REPLY_CREATED
                : NotificationType.COMMENT_CREATED;
        if (!recipient.equals(userId)) {
            notifier.dispatchToUser(
                    type,
                    recipient,
                    post.getCrew().getId(),
                    userId,
                    Map.of(
                            "postId", postId,
                            "commentId", saved.getId(),
                            "snippet", excerpt(request.content(), 80)));
        }
        return CommentResponse.from(saved, List.of(), List.of());
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, UpdateCommentRequest request) {
        Comment comment = loadComment(commentId);
        crewService.getCrewForMember(userId, comment.getPost().getCrew().getId());
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        comment.update(request.content());
        return CommentResponse.from(comment, List.of(), List.of());
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = loadComment(commentId);
        crewService.getCrewForMember(userId, comment.getPost().getCrew().getId());
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        commentRepository.delete(comment);
    }

    // ============ Reaction (토글) ============

    @Transactional
    public List<ReactionSummary> togglePostReaction(Long userId, Long postId, ReactionRequest request) {
        Post post = loadPost(postId);
        crewService.getCrewForMember(userId, post.getCrew().getId());
        Optional<Reaction> existing =
                reactionRepository.findByPostIdAndUserIdAndEmoji(postId, userId, request.emoji());
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            reactionRepository.save(Reaction.forPost(post, loadUser(userId), request.emoji()));
        }
        return summarizeReactions(reactionRepository.findAllByPostIdWithUser(postId), userId);
    }

    @Transactional
    public List<ReactionSummary> toggleCommentReaction(
            Long userId, Long commentId, ReactionRequest request) {
        Comment comment = loadComment(commentId);
        crewService.getCrewForMember(userId, comment.getPost().getCrew().getId());
        Optional<Reaction> existing = reactionRepository
                .findByCommentIdAndUserIdAndEmoji(commentId, userId, request.emoji());
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            reactionRepository.save(Reaction.forComment(comment, loadUser(userId), request.emoji()));
        }
        return summarizeReactions(
                reactionRepository.findAllByCommentIdInWithUser(List.of(commentId)), userId);
    }

    // ============ 헬퍼 ============

    private int clampPageSize(Integer size) {
        int s = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
        return Math.min(s, MAX_PAGE_SIZE);
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Post loadPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private Comment loadComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private String excerpt(String content, int len) {
        return content.length() <= len ? content : content.substring(0, len) + "…";
    }

    /**
     * 반응 목록을 (emoji, [userIds]) 형태로 그룹핑 + 내 반응 여부.
     * emoji 등장 순서를 유지하기 위해 LinkedHashMap 사용.
     */
    private List<ReactionSummary> summarizeReactions(List<Reaction> reactions, Long viewerId) {
        Map<String, List<Long>> grouped = new LinkedHashMap<>();
        for (Reaction r : reactions) {
            grouped.computeIfAbsent(r.getEmoji(), k -> new ArrayList<>()).add(r.getUser().getId());
        }
        List<ReactionSummary> summaries = new ArrayList<>(grouped.size());
        for (Map.Entry<String, List<Long>> e : grouped.entrySet()) {
            List<Long> userIds = e.getValue();
            summaries.add(new ReactionSummary(e.getKey(), userIds.size(), userIds, userIds.contains(viewerId)));
        }
        return summaries;
    }

    /** 댓글 리스트를 (parent → children) 트리로. 최상위 댓글은 id 오름차순, 대댓글도 id 오름차순. */
    private List<CommentResponse> buildCommentTree(
            List<Comment> allComments,
            Map<Long, List<Reaction>> reactionsByComment,
            Long viewerId) {
        Map<Long, List<Comment>> byParent = new TreeMap<>();
        List<Comment> roots = new ArrayList<>();
        for (Comment c : allComments) {
            if (c.getParent() == null) {
                roots.add(c);
            } else {
                byParent.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c);
            }
        }
        Collections.sort(roots, (a, b) -> Long.compare(a.getId(), b.getId()));

        Map<Long, CommentResponse> cache = new HashMap<>();
        List<CommentResponse> result = new ArrayList<>(roots.size());
        for (Comment root : roots) {
            result.add(toResponseRecursive(root, byParent, reactionsByComment, viewerId, cache));
        }
        return result;
    }

    private CommentResponse toResponseRecursive(
            Comment comment,
            Map<Long, List<Comment>> byParent,
            Map<Long, List<Reaction>> reactionsByComment,
            Long viewerId,
            Map<Long, CommentResponse> cache) {
        List<Comment> children = byParent.getOrDefault(comment.getId(), List.of());
        children.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        List<CommentResponse> childResponses = new ArrayList<>(children.size());
        for (Comment child : children) {
            childResponses.add(toResponseRecursive(
                    child, byParent, reactionsByComment, viewerId, cache));
        }
        List<ReactionSummary> reactionSummaries =
                summarizeReactions(reactionsByComment.getOrDefault(comment.getId(), List.of()), viewerId);

        // HIDDEN 댓글은 트리 위상은 유지하되 본문/반응/작성자를 마스킹.
        CommentResponse response;
        if (comment.isHidden()) {
            response = new CommentResponse(
                    comment.getId(),
                    comment.getPost().getId(),
                    comment.getParent() == null ? null : comment.getParent().getId(),
                    null,
                    "(숨김)",
                    "(관리자에 의해 숨겨진 댓글)",
                    List.of(),
                    childResponses,
                    comment.getCreatedAt());
        } else {
            response = CommentResponse.from(comment, reactionSummaries, childResponses);
        }
        cache.put(comment.getId(), response);
        return response;
    }
}
