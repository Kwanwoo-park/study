package spring.study.jwt.dto;

import spring.study.member.entity.Member;

import java.time.Instant;

public record MobileAuthResponse(Long result, String accessToken, String refreshToken, Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt, MobileMemberResponse member) {
    public MobileAuthResponse(Member member, AuthenticationTokens tokens) {
        this(
                member.getId(),
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessTokenExpiresAt(),
                tokens.refreshTokenExpiresAt(),
                new MobileMemberResponse(member)
        );
    }
}
