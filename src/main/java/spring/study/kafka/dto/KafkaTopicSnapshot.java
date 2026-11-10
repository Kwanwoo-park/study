package spring.study.kafka.dto;

import java.util.List;
import java.util.Map;

public record KafkaTopicSnapshot(String topic, List<KafkaPartitionHealth> partitions, Map<String, String> configs, String error) {}
