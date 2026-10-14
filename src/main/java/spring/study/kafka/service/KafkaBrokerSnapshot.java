package spring.study.kafka.service;

import spring.study.kafka.dto.KafkaTopicHealth;
import spring.study.kafka.dto.KafkaPartitionLag;
import java.time.Instant;
import java.util.List;

record KafkaBrokerSnapshot(Instant observedAt, String error, List<KafkaTopicHealth> topics, List<KafkaPartitionLag> lags, Long totalLag, List<String> warnings) {}
