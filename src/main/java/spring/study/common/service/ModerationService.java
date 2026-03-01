package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {
    private final ForbiddenService forbiddenService;
    private final NotificationService notificationService;
    private final MemberService memberService;
    private final RedisTemplate<String, String> stringRedisTemplate;

    public int validate(String checkStr, Member member, HttpServletRequest request) {
        if (checkStr == null || checkStr.isBlank()) return -99;

        int risk = forbiddenService.findWordList(Status.APPROVAL, checkStr);
        String key = "forbidden:user:" + member.getId();

        if (risk == 3) {
            notificationService.createNotification(
                    memberService.findAdministrator(),
                    member.getName() + "님이 금칙어를 사용하여 차단하였습니다",
                    Group.ADMIN
            );

            memberService.updateRole(member.getId(), Role.DENIED);

            request.getSession(false).invalidate();

            stringRedisTemplate.delete(key);
        } else if (risk != 0) {
            stringRedisTemplate.opsForValue().setIfAbsent(key, "0", Duration.ofDays(1));

            Long count = stringRedisTemplate.opsForValue().increment(key, risk);

            if (count != null && count >= 5) {
                risk = 3;

                notificationService.createNotification(
                        memberService.findAdministrator(),
                        member.getName() + "님이 금칙어를 사용하여 차단하였습니다",
                        Group.ADMIN
                );

                memberService.updateRole(member.getId(), Role.DENIED);

                request.getSession(false).invalidate();

                stringRedisTemplate.delete(key);
            }
        }

        return risk;
    }
}
