package spring.study.jwt.dto;

import java.time.Instant;

public record TokenClaims(Long memberId, String email, String role, String type, String jti, Instant expiresAt) {}
