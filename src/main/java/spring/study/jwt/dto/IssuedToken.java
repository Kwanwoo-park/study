package spring.study.jwt.dto;

import java.time.Instant;

public record IssuedToken(String value, String jti, Instant expiresAt) {}
