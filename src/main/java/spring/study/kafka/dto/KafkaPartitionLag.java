package spring.study.kafka.dto;

public record KafkaPartitionLag(String topic, int partition, String group, Long committedOffset, long endOffset, Long lag) {}
