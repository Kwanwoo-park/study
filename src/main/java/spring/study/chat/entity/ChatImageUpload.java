package spring.study.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_image_upload", indexes = @Index(name = "idx_chat_image_upload_expiry", columnList = "expires_at,id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatImageUpload {
    private enum State { UPLOADING, READY, EXPIRED }

    @Id
    @Getter
    @Column(length = 36)
    private String id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "room_id")
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private State state;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public ChatImageUpload(String id, Long memberId, Long roomId, LocalDateTime expiresAt) {
        this.id = id;
        this.memberId = memberId;
        this.roomId = roomId;
        this.expiresAt = expiresAt;
        this.state = State.UPLOADING;
    }

    public boolean markReady(LocalDateTime now) {
        if (state != State.UPLOADING || !expiresAt.isAfter(now)) return false;

        state = State.READY;
        expiresAt = now.plusHours(24);
        return true;
    }

    public boolean canAttach(Long memberId, Long roomId, LocalDateTime now) {
        return state == State.READY && expiresAt.isAfter(now)
                && this.memberId.equals(memberId) && (this.roomId == null || this.roomId.equals(roomId));
    }

    public void abandon(LocalDateTime now) {
        if (state == State.UPLOADING) expiresAt = now;
    }

    public boolean claimCleanup(LocalDateTime now) {
        if (expiresAt.isAfter(now)) return false;

        state = State.EXPIRED;
        expiresAt = now.plusMinutes(5);
        return true;
    }
}
