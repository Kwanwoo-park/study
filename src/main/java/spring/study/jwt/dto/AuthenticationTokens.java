package spring.study.jwt.dto;

import java.time.Instant;

public record AuthenticationTokens(String accessToken, String refreshToken, Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {}
