package spring.study.kafka.dto;

import java.util.List;
import java.util.Map;

public record KafkaTopicHealth(String topic, int partitions, int configuredPartitions, int replicas,
                               String minInSyncReplicas, String retentionMs, String cleanupPolicy,
                               long underReplicatedPartitions, List<KafkaPartitionHealth> partitionDetails,
                               Map<String, String> configs, String error) {}
