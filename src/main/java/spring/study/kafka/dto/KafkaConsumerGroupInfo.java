package spring.study.kafka.dto;

import java.util.List;

public record KafkaConsumerGroupInfo(String groupId, String state, Integer coordinatorId, String assignor,
                                     Integer memberCount, Long totalLag, List<KafkaConsumerMemberInfo> members,
                                     List<KafkaPartitionLag> lags, String error) {}
