package spring.study.kafka.dto;

import java.time.Instant;
import java.util.List;

public record KafkaOverview(Instant observedAt, String brokerError, List<KafkaTopicHealth> topics, List<KafkaPartitionLag> lags, Long totalLag,
                       long outboxPending, long oldestOutboxSeconds, long outboxFailed, long unresolvedDeadLetters, long replayPending, List<String> warnings) {}
