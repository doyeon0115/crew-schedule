package com.crewschedule.crew.dto;

import com.crewschedule.crew.domain.Crew;
import com.crewschedule.crew.domain.CrewMember;
import com.crewschedule.crew.domain.CrewRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 크루 API 요청/응답 DTO 모음. */
public final class CrewDtos {

    private CrewDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "크루 이름은 필수입니다.") @Size(max = 50) String name) {
    }

    public record JoinRequest(
            @NotBlank(message = "초대 코드는 필수입니다.") @Size(max = 12) String inviteCode) {
    }

    public record CrewResponse(Long id, String name, String inviteCode, Long ownerId, long memberCount) {

        public static CrewResponse of(Crew crew, long memberCount) {
            return new CrewResponse(
                    crew.getId(), crew.getName(), crew.getInviteCode(), crew.getOwner().getId(), memberCount);
        }
    }

    public record MemberResponse(Long userId, String nickname, String profileImageUrl, CrewRole role) {

        public static MemberResponse from(CrewMember member) {
            return new MemberResponse(
                    member.getUser().getId(),
                    member.getUser().getNickname(),
                    member.getUser().getProfileImageUrl(),
                    member.getRole());
        }
    }

    public record CrewDetailResponse(
            Long id, String name, String inviteCode, Long ownerId, List<MemberResponse> members) {

        public static CrewDetailResponse of(Crew crew, List<CrewMember> members) {
            return new CrewDetailResponse(
                    crew.getId(),
                    crew.getName(),
                    crew.getInviteCode(),
                    crew.getOwner().getId(),
                    members.stream().map(MemberResponse::from).toList());
        }
    }
}
