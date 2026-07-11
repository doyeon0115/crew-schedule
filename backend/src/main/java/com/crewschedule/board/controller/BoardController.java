package com.crewschedule.board.controller;

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
import com.crewschedule.board.service.BoardService;
import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Board", description = "크루 게시판 (글·댓글·대댓글·이모지 반응)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // ============ Post ============

    @Operation(summary = "게시글 작성")
    @PostMapping("/crews/{crewId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostSummary> createPost(
            @CurrentUserId Long userId,
            @PathVariable Long crewId,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.ok(boardService.createPost(userId, crewId, request));
    }

    @Operation(summary = "크루 게시글 목록 (최신순 페이지)")
    @GetMapping("/crews/{crewId}/posts")
    public ApiResponse<PostListResponse> listPosts(
            @CurrentUserId Long userId,
            @PathVariable Long crewId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(boardService.listPosts(userId, crewId, beforeId, size));
    }

    @Operation(summary = "게시글 상세(댓글·반응 트리 포함)")
    @GetMapping("/posts/{postId}")
    public ApiResponse<PostDetail> getPost(
            @CurrentUserId Long userId, @PathVariable Long postId) {
        return ApiResponse.ok(boardService.getPost(userId, postId));
    }

    @Operation(summary = "게시글 수정 (작성자만)")
    @PutMapping("/posts/{postId}")
    public ApiResponse<PostSummary> updatePost(
            @CurrentUserId Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        return ApiResponse.ok(boardService.updatePost(userId, postId, request));
    }

    @Operation(summary = "게시글 삭제 (작성자만)")
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @CurrentUserId Long userId, @PathVariable Long postId) {
        boardService.deletePost(userId, postId);
        return ApiResponse.ok(null);
    }

    // ============ Comment ============

    @Operation(summary = "댓글/대댓글 작성")
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponse> addComment(
            @CurrentUserId Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.ok(boardService.addComment(userId, postId, request));
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @CurrentUserId Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return ApiResponse.ok(boardService.updateComment(userId, commentId, request));
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @CurrentUserId Long userId, @PathVariable Long commentId) {
        boardService.deleteComment(userId, commentId);
        return ApiResponse.ok(null);
    }

    // ============ Reaction ============

    @Operation(summary = "게시글 이모지 반응 토글")
    @PostMapping("/posts/{postId}/reactions")
    public ApiResponse<List<ReactionSummary>> togglePostReaction(
            @CurrentUserId Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody ReactionRequest request) {
        return ApiResponse.ok(boardService.togglePostReaction(userId, postId, request));
    }

    @Operation(summary = "댓글 이모지 반응 토글")
    @PostMapping("/comments/{commentId}/reactions")
    public ApiResponse<List<ReactionSummary>> toggleCommentReaction(
            @CurrentUserId Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody ReactionRequest request) {
        return ApiResponse.ok(boardService.toggleCommentReaction(userId, commentId, request));
    }
}
