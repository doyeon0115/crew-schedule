package com.crewschedule.board.dto;

import com.crewschedule.board.domain.Comment;
import com.crewschedule.board.domain.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class BoardDtos {

    private BoardDtos() {}

    public record CreatePostRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 20_000) String content) {}

    public record UpdatePostRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 20_000) String content) {}

    public record CreateCommentRequest(
            @NotBlank @Size(max = 2000) String content,
            Long parentCommentId) {}

    public record UpdateCommentRequest(@NotBlank @Size(max = 2000) String content) {}

    public record ReactionRequest(@NotBlank @Size(max = 20) String emoji) {}

    public record PostSummary(
            Long id,
            Long crewId,
            Long authorId,
            String authorNickname,
            String title,
            String excerpt,
            int commentCount,
            LocalDateTime createdAt) {

        public static PostSummary from(Post p, int commentCount) {
            String content = p.getContent();
            String excerpt = content.length() <= 120 ? content : content.substring(0, 120) + "…";
            return new PostSummary(
                    p.getId(),
                    p.getCrew().getId(),
                    p.getAuthor().getId(),
                    p.getAuthor().getNickname(),
                    p.getTitle(),
                    excerpt,
                    commentCount,
                    p.getCreatedAt());
        }
    }

    public record PostListResponse(List<PostSummary> posts, Long nextBeforeId) {}

    public record ReactionSummary(String emoji, int count, List<Long> userIds, boolean myReaction) {}

    public record CommentResponse(
            Long id,
            Long postId,
            Long parentCommentId,
            Long authorId,
            String authorNickname,
            String content,
            List<ReactionSummary> reactions,
            List<CommentResponse> replies,
            LocalDateTime createdAt) {

        public static CommentResponse from(
                Comment c, List<ReactionSummary> reactions, List<CommentResponse> replies) {
            return new CommentResponse(
                    c.getId(),
                    c.getPost().getId(),
                    c.getParent() == null ? null : c.getParent().getId(),
                    c.getAuthor().getId(),
                    c.getAuthor().getNickname(),
                    c.getContent(),
                    reactions,
                    replies,
                    c.getCreatedAt());
        }
    }

    public record PostDetail(
            Long id,
            Long crewId,
            Long authorId,
            String authorNickname,
            String title,
            String content,
            List<ReactionSummary> reactions,
            List<CommentResponse> comments,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static PostDetail of(
                Post p,
                List<ReactionSummary> reactions,
                List<CommentResponse> comments) {
            return new PostDetail(
                    p.getId(),
                    p.getCrew().getId(),
                    p.getAuthor().getId(),
                    p.getAuthor().getNickname(),
                    p.getTitle(),
                    p.getContent(),
                    reactions,
                    comments,
                    p.getCreatedAt(),
                    p.getUpdatedAt());
        }
    }

    /** 내부용: 반응 그룹핑 결과 (에모지 → 유저ID 목록) */
    public record ReactionGrouping(Map<String, List<Long>> byEmoji) {}
}
