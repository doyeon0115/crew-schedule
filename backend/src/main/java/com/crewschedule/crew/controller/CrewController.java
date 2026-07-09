package com.crewschedule.crew.controller;

import com.crewschedule.common.web.ApiResponse;
import com.crewschedule.common.web.CurrentUserId;
import com.crewschedule.crew.dto.CrewDtos.CreateRequest;
import com.crewschedule.crew.dto.CrewDtos.CrewDetailResponse;
import com.crewschedule.crew.dto.CrewDtos.CrewResponse;
import com.crewschedule.crew.dto.CrewDtos.JoinRequest;
import com.crewschedule.crew.service.CrewService;
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

@Tag(name = "Crew", description = "크루(그룹) 생성·초대·조회")
@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;

    @Operation(summary = "크루 생성", description = "크루를 만들고 생성자를 OWNER로 가입시킨다. 고유 초대 코드가 발급된다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CrewResponse> create(
            @CurrentUserId Long userId, @Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok(crewService.create(userId, request.name()));
    }

    @Operation(summary = "초대 코드로 크루 가입")
    @PostMapping("/join")
    public ApiResponse<CrewResponse> join(
            @CurrentUserId Long userId, @Valid @RequestBody JoinRequest request) {
        return ApiResponse.ok(crewService.join(userId, request.inviteCode()));
    }

    @Operation(summary = "내 크루 목록")
    @GetMapping
    public ApiResponse<List<CrewResponse>> myCrews(@CurrentUserId Long userId) {
        return ApiResponse.ok(crewService.getMyCrews(userId));
    }

    @Operation(summary = "크루 상세(멤버 목록 포함)")
    @GetMapping("/{crewId}")
    public ApiResponse<CrewDetailResponse> detail(
            @CurrentUserId Long userId, @PathVariable Long crewId) {
        return ApiResponse.ok(crewService.getDetail(userId, crewId));
    }
}
