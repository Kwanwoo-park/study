package spring.study.kafka.dto;

import java.time.LocalDateTime;

public record KafkaDispatchResult(int processedCount, boolean batchFull, boolean failed, LocalDateTime retryAt) {
    public static KafkaDispatchResult empty() {
        return new KafkaDispatchResult(0, false, false, null);
    }

    public static KafkaDispatchResult completed(int processedCount, boolean batchFull) {
        return new KafkaDispatchResult(processedCount, batchFull, false, null);
    }

    public static KafkaDispatchResult failed(int processedCount, LocalDateTime retryAt) {
        return new KafkaDispatchResult(processedCount, false, true, retryAt);
    }

    public boolean hasWork() {
        return processedCount > 0 || failed;
    }
}
