package spring.study.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_ip")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockedIp {
    @Id
    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "blocked_at", nullable = false, updatable = false)
    private LocalDateTime blockedAt;

    @Column(name = "blocked_by_member_id", updatable = false)
    private Long blockedByMemberId;

    public BlockedIp(String ipAddress, Long blockedByMemberId) {
        this.ipAddress = ipAddress;
        this.blockedAt = LocalDateTime.now();
        this.blockedByMemberId = blockedByMemberId;
    }
}
