package spring.study.jwt.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenRotation {
    private final RefreshTokenService refreshTokens;
    private final MemberService members;
    private final MemberTokenCacheService cache;

    @Transactional
    public boolean rotate(String oldJti, String newJti, Member member, Duration ttl, String ipAddress) {
        if (!refreshTokens.replace(oldJti, newJti, member, ttl, ipAddress)) return false;

        members.updateLastLoginTime(member.getId());
        cache.save(member, ttl);
        return true;
    }
}
