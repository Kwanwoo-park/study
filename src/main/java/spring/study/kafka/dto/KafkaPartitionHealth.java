package spring.study.kafka.dto;

import java.util.List;

public record KafkaPartitionHealth(int partition, Integer leader, List<Integer> replicas,
                                   List<Integer> inSyncReplicas, Long startOffset, Long endOffset) {}
