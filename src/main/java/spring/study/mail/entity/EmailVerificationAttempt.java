package spring.study.mail.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "email_verification_attempt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationAttempt {
    private static final Duration REQUEST_WINDOW = Duration.ofDays(1);

    @Id
    @Column(name = "email_digest", length = 64, updatable = false)
    private String emailDigest;

    @Column(name = "previous_request_at")
    private Instant previousRequestAt;

    @Column(name = "last_request_at")
    private Instant lastRequestAt;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_email", length = 254)
    private String blockedEmail;

    public boolean allowRequest(Instant now, String emailAddress) {
        if (blockedAt != null || (previousRequestAt != null && !previousRequestAt.isBefore(now.minus(REQUEST_WINDOW)))) {
            block(now, emailAddress);
            return false;
        }

        previousRequestAt = lastRequestAt;
        lastRequestAt = now;
        return true;
    }

    public void block(Instant now, String emailAddress) {
        if (blockedAt == null) blockedAt = now;
        if (blockedEmail == null) blockedEmail = emailAddress;
    }
}
