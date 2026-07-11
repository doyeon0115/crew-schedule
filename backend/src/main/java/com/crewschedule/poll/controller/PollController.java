package com.crewschedule.poll.controller;

import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import com.crewschedule.poll.dto.PollDtos.CreatePollRequest;
import com.crewschedule.poll.dto.PollDtos.PollResponse;
import com.crewschedule.poll.service.PollService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Poll", description = "날짜 투표 생성·투표·마감")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @Operation(summary = "투표 생성 (크루 멤버)")
    @PostMapping("/crews/{crewId}/polls")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PollResponse> create(
            @CurrentUserId Long userId,
            @PathVariable Long crewId,
            @Valid @RequestBody CreatePollRequest request) {
        return ApiResponse.ok(pollService.create(userId, crewId, request));
    }

    @Operation(summary = "크루 투표 목록")
    @GetMapping("/crews/{crewId}/polls")
    public ApiResponse<List<PollResponse>> list(
            @CurrentUserId Long userId, @PathVariable Long crewId) {
        return ApiResponse.ok(pollService.listByCrew(userId, crewId));
    }

    @Operation(summary = "투표 상세 (후보별 득표수 + 내 투표 여부)")
    @GetMapping("/polls/{pollId}")
    public ApiResponse<PollResponse> detail(
            @CurrentUserId Long userId, @PathVariable Long pollId) {
        return ApiResponse.ok(pollService.getDetail(userId, pollId));
    }

    @Operation(summary = "후보에 투표")
    @PostMapping("/polls/{pollId}/candidates/{candidateId}/vote")
    public ApiResponse<PollResponse> vote(
            @CurrentUserId Long userId,
            @PathVariable Long pollId,
            @PathVariable Long candidateId) {
        return ApiResponse.ok(pollService.vote(userId, pollId, candidateId));
    }

    @Operation(summary = "투표 취소(멱등)")
    @DeleteMapping("/polls/{pollId}/candidates/{candidateId}/vote")
    public ApiResponse<PollResponse> unvote(
            @CurrentUserId Long userId,
            @PathVariable Long pollId,
            @PathVariable Long candidateId) {
        return ApiResponse.ok(pollService.unvote(userId, pollId, candidateId));
    }

    @Operation(summary = "투표 마감 (생성자만) — 최다 득표 후보를 winner로 확정")
    @PostMapping("/polls/{pollId}/close")
    public ApiResponse<PollResponse> close(
            @CurrentUserId Long userId, @PathVariable Long pollId) {
        return ApiResponse.ok(pollService.close(userId, pollId));
    }
}
