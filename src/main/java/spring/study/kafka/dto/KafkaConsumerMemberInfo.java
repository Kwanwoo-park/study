package spring.study.kafka.dto;

import java.util.List;

public record KafkaConsumerMemberInfo(String consumerId, String clientId, String host, List<String> assignments) {}
