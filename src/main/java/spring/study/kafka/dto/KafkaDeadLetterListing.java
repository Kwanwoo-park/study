package spring.study.kafka.dto;

import java.util.List;

public record KafkaDeadLetterListing(List<KafkaDeadLetterEntry> entries, Long nextBeforeId) {}
