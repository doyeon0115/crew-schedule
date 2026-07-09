package com.crewschedule.meetup.controller;

import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import com.crewschedule.meetup.dto.MeetupDtos.CreateRequest;
import com.crewschedule.meetup.dto.MeetupDtos.MeetupResponse;
import com.crewschedule.meetup.dto.MeetupDtos.RsvpRequest;
import com.crewschedule.meetup.service.MeetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Meetup", description = "약속 제안·RSVP·확정·취소")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeetupController {

    private final MeetupService meetupService;

    @Operation(summary = "약속 제안", description = "participantUserIds가 비어 있으면 크루 전원을 초대한다.")
    @PostMapping("/crews/{crewId}/meetups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MeetupResponse> propose(
            @CurrentUserId Long userId, @PathVariable Long crewId, @Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok(meetupService.propose(userId, crewId, request));
    }

    @Operation(summary = "크루 약속 목록")
    @GetMapping("/crews/{crewId}/meetups")
    public ApiResponse<List<MeetupResponse>> crewMeetups(
            @CurrentUserId Long userId, @PathVariable Long crewId) {
        return ApiResponse.ok(meetupService.getCrewMeetups(userId, crewId));
    }

    @Operation(summary = "참석 여부 응답 (참석/미정/불참)")
    @PostMapping("/meetups/{meetupId}/rsvp")
    public ApiResponse<MeetupResponse> respond(
            @CurrentUserId Long userId, @PathVariable Long meetupId, @Valid @RequestBody RsvpRequest request) {
        return ApiResponse.ok(meetupService.respond(userId, meetupId, request.rsvp()));
    }

    @Operation(summary = "약속 확정 (생성자만)")
    @PostMapping("/meetups/{meetupId}/confirm")
    public ApiResponse<MeetupResponse> confirm(@CurrentUserId Long userId, @PathVariable Long meetupId) {
        return ApiResponse.ok(meetupService.confirm(userId, meetupId));
    }

    @Operation(summary = "약속 취소 (생성자만)")
    @PostMapping("/meetups/{meetupId}/cancel")
    public ApiResponse<MeetupResponse> cancel(@CurrentUserId Long userId, @PathVariable Long meetupId) {
        return ApiResponse.ok(meetupService.cancel(userId, meetupId));
    }
}
