package spring.study.kafka.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.dto.*;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.kafka.service.KafkaDeadLetterService;
import spring.study.kafka.service.KafkaClusterService;
import spring.study.kafka.service.KafkaOutboxEventService;
import spring.study.member.entity.Member;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaOperationsFacade {
    private final KafkaClusterService clusterService;
    private final KafkaOperationsProperties properties;
    private final KafkaDeadLetterService deadLetters;
    private final KafkaOutboxEventService outboxEventService;
    private final IntegrationEventLogService eventLogService;

    public ResponseEntity<?> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken()
        ));
    }

    public ResponseEntity<?> overview() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(kafkaOverview());
    }

    public ResponseEntity<?> list(KafkaDeadLetterStatus status, Long beforeId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(deadLetters.list(status, beforeId));
    }

    public ResponseEntity<?> replay(long id, Member administrator) {
        KafkaDeadLetter entry = deadLetters.requestReplay(id, administrator.getId());

        eventLogService.afterCommit(IntegrationEventLogRoute.kafka(entry.getSourceTopic()), IntegrationEventLogOperation.REPLAY, IntegrationEventLogOutcome.QUEUED, entry.getId(), 1, 0, null);

        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("message", "재발행 요청을 저장했습니다. 처리 상태를 확인해 주세요."));
    }

    private KafkaOverview kafkaOverview() {
        KafkaBrokerSnapshot broker = clusterService.snapshot();
        List<String> warnings = new ArrayList<>(broker.warnings());
        List<KafkaTopicHealth> topics = broker.topics().stream().map(topic -> health(topic, warnings)).toList();
        LocalDateTime oldest = outboxEventService.findFirst();
        long age = oldest == null ? 0 : Math.max(0, Duration.between(oldest, LocalDateTime.now()).toSeconds());
        long failed = outboxEventService.countByDeadLetteredTrue();
        long unresolved = deadLetters.countByStatusIn(List.of(KafkaDeadLetterStatus.NEW, KafkaDeadLetterStatus.REPLAY_FAILED));

        if (broker.totalLag() != null && broker.totalLag() >= properties.getLagWarningThreshold()) warnings.add("Kafka 소비 지연이 " + broker.totalLag() + "건입니다.");
        if (age >= properties.getOutboxWarningSeconds()) warnings.add("발행 대기 중 가장 오래된 이벤트가 " + age + "초 지연되고 있습니다.");
        if (failed > 0) warnings.add("Outbox 최종 실패 " + failed + "건이 있습니다.");
        if (unresolved > 0) warnings.add("확인이 필요한 소비 실패 기록 " + unresolved + "건이 있습니다.");

        return new KafkaOverview(broker.observedAt(), broker.error(), topics, broker.lags(), broker.totalLag(),
                outboxEventService.countByDeadLetteredFalse(), age, failed, unresolved,
                deadLetters.countByStatusIn(List.of(KafkaDeadLetterStatus.REPLAY_PENDING)), List.copyOf(warnings),
                broker.clusterId(), broker.controllerId(), broker.brokers(), broker.consumerGroups());
    }

    private KafkaTopicHealth health(KafkaTopicSnapshot topic, List<String> warnings) {
        String name = topic.topic();
        int desired = name.endsWith(".DLT") ? properties.getDeadLetterPartitions() : name.equals(KafkaTopics.CHAT) ? properties.getChatPartitions() : properties.getNotificationPartitions();
        int replicas = topic.partitions().stream().mapToInt(partition -> partition.replicas().size()).min().orElse(0);
        long underReplicated = topic.partitions().stream().filter(p -> p.inSyncReplicas().size() < p.replicas().size()).count();
        long leaderless = topic.partitions().stream().filter(partition -> partition.leader() == null).count();
        Map<String, String> configs = topic.configs();

        if (!topic.partitions().isEmpty()) {
            if (underReplicated > 0) warnings.add(name + ": 복제가 지연되는 파티션이 있습니다.");
            if (leaderless > 0) warnings.add(name + ": 리더가 없는 파티션이 " + leaderless + "개 있습니다.");
            if (topic.partitions().size() != desired || replicas != properties.getReplicationFactor()) warnings.add(name + ": 운영 토픽 구성과 신규 생성 설정이 다릅니다. 기존 구성은 자동 변경하지 않습니다.");

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
