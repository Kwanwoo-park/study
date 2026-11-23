package spring.study.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "login_ip_attempt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginIpAttempt {
    @Id
    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    public int recordFailure() {
        return ++failedAttempts;
    }

    public void resetFailures() {
        failedAttempts = 0;
    }
}
