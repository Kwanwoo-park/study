package spring.study.jwt.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.service.MemberService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenFacade {
    private static final long INACTIVE_MEMBER_DAYS = 15L;

    private final MemberService memberService;
    private final MemberTokenCacheService memberTokenCacheService;
    private final RefreshTokenService refreshTokenService;

    public void cleanUp() {
        Instant now = Instant.now();
        List<RefreshToken> expiredTokens = refreshTokenService.findByExpiresAtLessThanEqual(now);

        refreshTokenService.deleteAllInBatch(expiredTokens);

        expiredTokens.stream()
                .map(RefreshToken::getMemberId)
                .distinct()
                .forEach(memberId -> deleteMemberCacheIfNoValidToken(memberId, now));

        List<Long> tokenMemberIds = refreshTokenService.findDistinctMemberIds();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(INACTIVE_MEMBER_DAYS);
        List<Long> inactiveMemberIds = memberService.findInactiveMemberIds(tokenMemberIds, cutoff);

        inactiveMemberIds.forEach(refreshTokenService::revoke);
        inactiveMemberIds.forEach(memberTokenCacheService::delete);
    }

    private void deleteMemberCacheIfNoValidToken(Long memberId, Instant now) {
        if (!refreshTokenService.existsByMemberIdAndExpiresAtAfter(memberId, now)) {
            memberTokenCacheService.delete(memberId);
        }
    }
}
