package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.kafka.dto.*;
import spring.study.kafka.repository.KafkaClusterRepository;

@Service
@RequiredArgsConstructor
public class KafkaClusterService {
    private final KafkaClusterRepository cluster;

    public KafkaBrokerSnapshot snapshot() {
        return cluster.snapshot();
    }
}
