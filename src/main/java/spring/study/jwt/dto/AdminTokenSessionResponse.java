package spring.study.jwt.dto;

import spring.study.common.dto.IpLocationResponse;
import spring.study.jwt.entity.RefreshToken;

import java.time.Instant;

public record AdminTokenSessionResponse(
        Long memberId,
        String memberEmail,
        String ipAddress,
        IpLocationResponse ipLocation,
        Instant expiresAt
) {
    public AdminTokenSessionResponse(
            RefreshToken token,
            String memberEmail,
            IpLocationResponse ipLocation
    ) {
        this(
                token.getMemberId(),
                memberEmail,
                token.getIpAddress() == null || token.getIpAddress().isBlank()
                        ? "UNKNOWN"
                        : token.getIpAddress(),
                ipLocation,
                token.getExpiresAt()
        );
    }
}
