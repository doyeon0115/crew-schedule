package com.crewschedule.user.service;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.dto.UserDtos.ProfileResponse;
import com.crewschedule.user.dto.UserDtos.UpdateProfileRequest;
import com.crewschedule.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return ProfileResponse.from(findUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.updateProfile(request.nickname(), request.profileImageUrl());
        return ProfileResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
