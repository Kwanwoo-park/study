package spring.study.kafka.dto;

public record KafkaTopicHealth(String topic, int partitions, int configuredPartitions, int replicas, String minInSyncReplicas, String retentionMs, String cleanupPolicy, long underReplicatedPartitions) {}
