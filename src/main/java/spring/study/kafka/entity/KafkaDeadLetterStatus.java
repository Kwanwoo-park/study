package spring.study.kafka.entity;

public enum KafkaDeadLetterStatus {
    NEW, REPLAY_PENDING, REPLAYED, REPLAY_FAILED
}
