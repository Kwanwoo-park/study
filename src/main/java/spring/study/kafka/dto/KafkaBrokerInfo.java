package spring.study.kafka.dto;

public record KafkaBrokerInfo(int id, String host, int port, String rack, boolean controller, long leaderPartitions, long replicaPartitions) {}
