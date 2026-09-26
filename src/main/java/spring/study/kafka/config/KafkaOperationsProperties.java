package spring.study.kafka.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter @Setter
@Component @Validated
@ConfigurationProperties(prefix = "kafka.operations")
public class KafkaOperationsProperties {
    private boolean provisionTopics = true;
    @Min(1) @Max(100) private int chatPartitions = 1;
    @Min(1) @Max(100) private int notificationPartitions = 1;
    @Min(1) @Max(100) private int deadLetterPartitions = 1;
    @Min(1) @Max(10) private short replicationFactor = 1;
    @Min(1) @Max(10) private int minInSyncReplicas = 1;
    @Min(1) @Max(365) private int retentionDays = 7;
    @Min(1) @Max(365) private int deadLetterRetentionDays = 14;
    @Min(0) @Max(10) private long consumerRetries = 3;
    @Min(100) @Max(10000) private long consumerRetryDelayMs = 1000;
    @Min(1) @Max(20) private int chatConcurrency = 1;
    @Min(1) @Max(20) private int notificationConcurrency = 1;
    @Min(1) private long lagWarningThreshold = 100;
    @Min(1) private long outboxWarningSeconds = 60;
    @Min(60) private long dedupTtlSeconds = 2592000;
    @Min(1) @Max(365) private int replayHistoryDays = 30;

    @AssertTrue(message = "min-in-sync-replicas must not exceed replication-factor")
    public boolean isReplicaConfigurationValid() { return minInSyncReplicas <= replicationFactor; }
}
