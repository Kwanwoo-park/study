package spring.study.kafka.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Getter @NoArgsConstructor
@Table(name = "kafka_dead_letter", uniqueConstraints = @UniqueConstraint(name = "uk_kafka_dlt_source",
        columnNames = {"source_topic", "source_partition", "source_offset"}),
        indexes = @Index(name = "idx_kafka_dlt_replay", columnList = "status,next_attempt_at,id"))
public class KafkaDeadLetter {
    public enum Status { NEW, REPLAY_PENDING, REPLAYED, REPLAY_FAILED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "source_topic", nullable = false, length = 100) private String sourceTopic;
    @Column(name = "source_partition", nullable = false) private int sourcePartition;
    @Column(name = "source_offset", nullable = false) private long sourceOffset;
    @Column(name = "event_key", length = 255) private String eventKey;
    @Column(name = "event_id", length = 255) private String eventId;
    @Lob @Column(columnDefinition = "longblob") private byte[] payload;
    @Column(name = "error_type", length = 255) private String errorType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "replayed_at") private LocalDateTime replayedAt;
    @Column(name = "next_attempt_at") private LocalDateTime nextAttemptAt;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "requested_by") private Long requestedBy;

    public KafkaDeadLetter(String topic, int partition, long offset, String key, String eventId, byte[] payload, String errorType) {
        sourceTopic = topic; sourcePartition = partition; sourceOffset = offset; eventKey = key;
        this.eventId = eventId; this.payload = payload; this.errorType = errorType;
        status = Status.NEW; createdAt = LocalDateTime.now();
    }
    public void requestReplay(Long memberId) {
        if (status != Status.NEW && status != Status.REPLAY_FAILED) throw new IllegalStateException("이미 재발행 요청이 처리된 기록입니다.");
        if (payload == null) throw new IllegalStateException("본문이 없는 이벤트는 재발행할 수 없습니다.");
        status = Status.REPLAY_PENDING; requestedBy = memberId; attemptCount = 0; nextAttemptAt = LocalDateTime.now();
    }
    public void replayed() {
        status = Status.REPLAYED; attemptCount++; replayedAt = LocalDateTime.now(); nextAttemptAt = null;
    }
    public void replayFailed(Exception error) {
        attemptCount++;
        errorType = error.getClass().getName();
        status = attemptCount >= 10 ? Status.REPLAY_FAILED : Status.REPLAY_PENDING;
        nextAttemptAt = status == Status.REPLAY_FAILED ? null : LocalDateTime.now().plusSeconds(Math.min(1800, 5L << Math.min(20, attemptCount - 1)));
    }
}
