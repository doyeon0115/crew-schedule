package com.crewschedule.crew.service;

import com.crewschedule.common.exception.BusinessException;
import com.crewschedule.common.exception.ErrorCode;
import com.crewschedule.crew.domain.Crew;
import com.crewschedule.crew.domain.CrewMember;
import com.crewschedule.crew.domain.CrewRole;
import com.crewschedule.crew.dto.CrewDtos.CrewDetailResponse;
import com.crewschedule.crew.dto.CrewDtos.CrewResponse;
import com.crewschedule.crew.repository.CrewMemberRepository;
import com.crewschedule.crew.repository.CrewRepository;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrewService {

    private static final int INVITE_CODE_MAX_RETRY = 5;

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final UserRepository userRepository;
    private final InviteCodeGenerator inviteCodeGenerator;

    /** 크루를 생성하고 생성자를 OWNER로 가입시킨다. */
    @Transactional
    public CrewResponse create(Long userId, String name) {
        User owner = getUser(userId);
        Crew crew = Crew.builder()
                .name(name)
                .inviteCode(generateUniqueInviteCode())
                .owner(owner)
                .build();
        crewRepository.save(crew);
        crewMemberRepository.save(
                CrewMember.builder().crew(crew).user(owner).role(CrewRole.OWNER).build());
        return CrewResponse.of(crew, 1);
    }

    /** 초대 코드로 크루에 가입한다. */
    @Transactional
    public CrewResponse join(Long userId, String inviteCode) {
        User user = getUser(userId);
        Crew crew = crewRepository
                .findByInviteCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));
        if (crewMemberRepository.existsByCrewIdAndUserId(crew.getId(), userId)) {
            throw new BusinessException(ErrorCode.ALREADY_CREW_MEMBER);
        }
        crewMemberRepository.save(
                CrewMember.builder().crew(crew).user(user).role(CrewRole.MEMBER).build());
        return CrewResponse.of(crew, crewMemberRepository.countByCrewId(crew.getId()));
    }

    /** 내가 가입한 크루 목록. */
    public List<CrewResponse> getMyCrews(Long userId) {
        return crewMemberRepository.findAllWithCrewByUserId(userId).stream()
                .map(cm -> CrewResponse.of(cm.getCrew(), crewMemberRepository.countByCrewId(cm.getCrew().getId())))
                .toList();
    }

    /** 크루 상세(멤버 목록 포함). 멤버만 조회할 수 있다. */
    public CrewDetailResponse getDetail(Long userId, Long crewId) {
        Crew crew = getCrewForMember(userId, crewId);
        return CrewDetailResponse.of(crew, crewMemberRepository.findAllWithUserByCrewId(crewId));
    }

    /** 크루 존재 + 요청자의 멤버십을 확인하고 크루를 반환한다. 다른 서비스에서도 사용한다. */
    public Crew getCrewForMember(Long userId, Long crewId) {
        Crew crew = crewRepository
                .findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        if (!crewMemberRepository.existsByCrewIdAndUserId(crewId, userId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_MEMBER);
        }
        return crew;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < INVITE_CODE_MAX_RETRY; i++) {
            String code = inviteCodeGenerator.generate();
            if (!crewRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("초대 코드 생성에 실패했습니다. 다시 시도해 주세요.");
    }
}
