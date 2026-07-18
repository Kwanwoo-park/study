package spring.study.member.jwt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Getter
@Entity
@Table(
        name = "refresh_token",
        indexes = {
                @Index(name = "idx_refresh_token_member_id", columnList = "member_id"),
                @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String jti;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    public RefreshToken(String jti, Long memberId, Duration ttl) {
        this.jti = jti;
        this.memberId = memberId;
        this.expiresAt = Instant.now().plus(ttl);
    }
}
