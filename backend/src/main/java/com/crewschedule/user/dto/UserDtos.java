package com.crewschedule.user.dto;

import com.crewschedule.user.domain.AuthProvider;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {}

    public record ProfileResponse(
            Long id,
            String email,
            String nickname,
            String profileImageUrl,
            UserRole role,
            AuthProvider provider) {
        public static ProfileResponse from(User user) {
            return new ProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getRole(),
                    user.getProvider());
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 50) String nickname,
            @Size(max = 500) String profileImageUrl) {}
}
