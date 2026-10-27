package spring.study.kafka.dto;

import java.time.Instant;
import java.util.List;

public record KafkaBrokerSnapshot(Instant observedAt, String error, String clusterId, Integer controllerId,
                                  List<KafkaBrokerInfo> brokers, List<KafkaTopicSnapshot> topics,
                                  List<KafkaConsumerGroupInfo> consumerGroups, List<KafkaPartitionLag> lags,
                                  Long totalLag, List<String> warnings) {}
