package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.dto.KafkaOverview;
import spring.study.kafka.dto.OutboxCreatedAtProjection;
import spring.study.kafka.dto.KafkaTopicHealth;
import spring.study.kafka.dto.KafkaTopicSnapshot;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.kafka.repository.KafkaClusterRepository;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import spring.study.kafka.repository.KafkaOutboxEventRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class KafkaMonitoringService {
    private final KafkaClusterRepository cluster;
    private final KafkaOperationsProperties properties;
    private final KafkaOutboxEventRepository outbox;
    private final KafkaDeadLetterRepository deadLetters;

    public KafkaOverview overview() {
        var broker = cluster.snapshot();
        List<String> warnings = new ArrayList<>(broker.warnings());
        List<KafkaTopicHealth> topics = broker.topics().stream().map(topic -> health(topic, warnings)).toList();
        LocalDateTime oldest = outbox.findFirstByDeadLetteredFalseAndCreatedAtIsNotNullOrderByCreatedAtAsc()
                .map(OutboxCreatedAtProjection::createdAt).orElse(null);
        long age = oldest == null ? 0 : Math.max(0, Duration.between(oldest, LocalDateTime.now()).toSeconds());
        long failed = outbox.countByDeadLetteredTrue();
        long unresolved = deadLetters.countByStatusIn(List.of(KafkaDeadLetterStatus.NEW, KafkaDeadLetterStatus.REPLAY_FAILED));

        if (broker.totalLag() != null && broker.totalLag() >= properties.getLagWarningThreshold())
            warnings.add("Kafka 소비 지연이 " + broker.totalLag() + "건입니다.");
        if (age >= properties.getOutboxWarningSeconds())
            warnings.add("발행 대기 중 가장 오래된 이벤트가 " + age + "초 지연되고 있습니다.");
        if (failed > 0) warnings.add("Outbox 최종 실패 " + failed + "건이 있습니다.");
        if (unresolved > 0) warnings.add("확인이 필요한 소비 실패 기록 " + unresolved + "건이 있습니다.");

        return new KafkaOverview(broker.observedAt(), broker.error(), topics, broker.lags(), broker.totalLag(),
                outbox.countByDeadLetteredFalse(), age, failed, unresolved,
                deadLetters.countByStatusIn(List.of(KafkaDeadLetterStatus.REPLAY_PENDING)), List.copyOf(warnings),
                broker.clusterId(), broker.controllerId(), broker.brokers(), broker.consumerGroups());
    }

    private KafkaTopicHealth health(KafkaTopicSnapshot topic, List<String> warnings) {
        String name = topic.topic();
        int desired = name.endsWith(".DLT") ? properties.getDeadLetterPartitions()
                : name.equals(KafkaTopics.CHAT) ? properties.getChatPartitions() : properties.getNotificationPartitions();
        int replicas = topic.partitions().stream().mapToInt(partition -> partition.replicas().size()).min().orElse(0);
        long underReplicated = topic.partitions().stream().filter(p -> p.inSyncReplicas().size() < p.replicas().size()).count();
        long leaderless = topic.partitions().stream().filter(partition -> partition.leader() == null).count();
        var configs = topic.configs();
        if (!topic.partitions().isEmpty()) {
            if (underReplicated > 0) warnings.add(name + ": 복제가 지연되는 파티션이 있습니다.");
            if (leaderless > 0) warnings.add(name + ": 리더가 없는 파티션이 " + leaderless + "개 있습니다.");
            if (topic.partitions().size() != desired || replicas != properties.getReplicationFactor())
                warnings.add(name + ": 운영 토픽 구성과 신규 생성 설정이 다릅니다. 기존 구성은 자동 변경하지 않습니다.");
            long retention = TimeUnit.DAYS.toMillis(name.endsWith(".DLT") ? properties.getDeadLetterRetentionDays() : properties.getRetentionDays());
            if (topic.error() == null && (!"delete".equals(configs.get("cleanup.policy"))
                    || !Long.toString(retention).equals(configs.get("retention.ms"))
                    || !Integer.toString(properties.getMinInSyncReplicas()).equals(configs.get("min.insync.replicas"))))
                warnings.add(name + ": 보관/복제 정책이 설정값과 다릅니다.");
        }
        return new KafkaTopicHealth(name, topic.partitions().size(), desired, replicas,
                configs.get("min.insync.replicas"), configs.get("retention.ms"), configs.get("cleanup.policy"),
                underReplicated, topic.partitions(), configs, topic.error());
    }
}
