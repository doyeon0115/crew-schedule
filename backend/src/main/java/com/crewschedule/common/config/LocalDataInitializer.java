package com.crewschedule.common.config;

import com.crewschedule.crew.domain.Crew;
import com.crewschedule.crew.domain.CrewMember;
import com.crewschedule.crew.domain.CrewRole;
import com.crewschedule.crew.repository.CrewMemberRepository;
import com.crewschedule.crew.repository.CrewRepository;
import com.crewschedule.schedule.domain.WeeklySlot;
import com.crewschedule.schedule.repository.WeeklySlotRepository;
import com.crewschedule.user.domain.User;
import com.crewschedule.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * local 프로필 개발용 시드 데이터. 프론트 목업(lib/mock.ts)과 동일한
 * 4인 크루 '우리끼리'를 만들어 수동 테스트와 프론트 연동을 돕는다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final WeeklySlotRepository weeklySlotRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User jiyoung = saveUser("jiyoung@crew.local", "지영");
        User yujin = saveUser("yujin@crew.local", "유진");
        User sunwoo = saveUser("sunwoo@crew.local", "선우");
        User sumin = saveUser("sumin@crew.local", "수민");

        Crew crew = crewRepository.save(
                Crew.builder().name("우리끼리").inviteCode("CREW2026").owner(jiyoung).build());
        crewMemberRepository.save(CrewMember.builder().crew(crew).user(jiyoung).role(CrewRole.OWNER).build());
        for (User user : List.of(yujin, sunwoo, sumin)) {
            crewMemberRepository.save(CrewMember.builder().crew(crew).user(user).role(CrewRole.MEMBER).build());
        }

        // 프론트 lib/mock.ts와 동일한 주간 스케줄
        saveWeek(jiyoung, Map.of(
                DayOfWeek.MONDAY, work("09:00", "18:00"),
                DayOfWeek.TUESDAY, work("09:00", "18:00"),
                DayOfWeek.WEDNESDAY, work("09:00", "18:00"),
                DayOfWeek.FRIDAY, work("09:00", "18:00")));
        saveWeek(yujin, Map.of(
                DayOfWeek.TUESDAY, work("09:00", "20:30"),
                DayOfWeek.WEDNESDAY, work("09:00", "20:30"),
                DayOfWeek.THURSDAY, work("09:00", "17:00"),
                DayOfWeek.FRIDAY, work("09:00", "20:30"),
                DayOfWeek.SATURDAY, work("09:00", "20:30")));
        saveWeek(sunwoo, Map.of(
                DayOfWeek.MONDAY, work("10:00", "19:00"),
                DayOfWeek.TUESDAY, work("10:00", "19:00"),
                DayOfWeek.THURSDAY, work("10:00", "16:00"),
                DayOfWeek.FRIDAY, work("10:00", "19:00")));
        saveWeek(sumin, Map.of(
                DayOfWeek.TUESDAY, work("13:00", "22:00"),
                DayOfWeek.WEDNESDAY, work("13:00", "22:00"),
                DayOfWeek.THURSDAY, work("09:00", "15:00"),
                DayOfWeek.SATURDAY, work("13:00", "22:00"),
                DayOfWeek.SUNDAY, work("13:00", "22:00")));

        log.info("Local seed data created: crew '우리끼리' (inviteCode=CREW2026, userIds 1~4)");
    }

    private User saveUser(String email, String nickname) {
        return userRepository.save(User.builder().email(email).nickname(nickname).build());
    }

    private record WorkHours(LocalTime start, LocalTime end) {
    }

    private static WorkHours work(String start, String end) {
        return new WorkHours(LocalTime.parse(start), LocalTime.parse(end));
    }

    /** 지정한 요일은 근무, 나머지 요일은 휴무로 저장한다. */
    private void saveWeek(User user, Map<DayOfWeek, WorkHours> workDays) {
        for (DayOfWeek day : DayOfWeek.values()) {
            WorkHours hours = workDays.get(day);
            weeklySlotRepository.save(WeeklySlot.builder()
                    .user(user)
                    .dayOfWeek(day)
                    .isOff(hours == null)
                    .startTime(hours != null ? hours.start() : null)
                    .endTime(hours != null ? hours.end() : null)
                    .build());
        }
    }
}
