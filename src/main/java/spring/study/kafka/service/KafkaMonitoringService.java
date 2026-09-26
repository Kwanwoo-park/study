package spring.study.kafka.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.entity.KafkaDeadLetter.Status;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaMonitoringService {
    private final Admin admin;
    private final KafkaOperationsProperties properties;
    private final KafkaOutboxEventRepository outbox;
    private final KafkaDeadLetterRepository deadLetters;
    private final String group;
    private BrokerSnapshot cached;
    private long cacheExpires;

    public KafkaMonitoringService(Admin admin, KafkaOperationsProperties properties, KafkaOutboxEventRepository outbox,
            KafkaDeadLetterRepository deadLetters, @Value("${group-id}") String group) {
        this.admin = admin; this.properties = properties; this.outbox = outbox; this.deadLetters = deadLetters; this.group = group;
    }

    public Overview overview() {
        BrokerSnapshot broker = brokerSnapshot();
        LocalDateTime oldest = outbox.findOldestPendingTime();
        long age = oldest == null ? 0 : Math.max(0, Duration.between(oldest, LocalDateTime.now()).toSeconds());
        long failed = outbox.countByDeadLetteredTrue();
        long unresolved = deadLetters.countByStatusIn(List.of(Status.NEW, Status.REPLAY_FAILED));
        List<String> warnings = new ArrayList<>(broker.warnings());
        if (age >= properties.getOutboxWarningSeconds()) warnings.add("발행 대기 중 가장 오래된 이벤트가 " + age + "초 지연되고 있습니다.");
        if (failed > 0) warnings.add("Outbox 최종 실패 " + failed + "건이 있습니다.");
        if (unresolved > 0) warnings.add("확인이 필요한 소비 실패 기록 " + unresolved + "건이 있습니다.");
        return new Overview(broker.observedAt(), broker.error(), broker.topics(), broker.lags(), broker.totalLag(),
                outbox.countByDeadLetteredFalse(), age, failed, unresolved,
                deadLetters.countByStatusIn(List.of(Status.REPLAY_PENDING)), warnings);
    }

    private synchronized BrokerSnapshot brokerSnapshot() {
        if (cached != null && System.nanoTime() < cacheExpires) return cached;
        List<TopicHealth> topics = new ArrayList<>(); List<PartitionLag> lags = new ArrayList<>(); List<String> warnings = new ArrayList<>();
        String error = null; Long totalLag = null;
        try {
            var described = admin.describeTopics(KafkaTopics.ALL).allTopicNames().get(5, TimeUnit.SECONDS);
            Map<TopicPartition, OffsetSpec> offsetRequests = new HashMap<>();
            described.values().forEach(t -> t.partitions().forEach(p -> offsetRequests.put(new TopicPartition(t.name(), p.partition()), OffsetSpec.latest())));
            var resources = KafkaTopics.ALL.stream().map(t -> new ConfigResource(ConfigResource.Type.TOPIC, t)).toList();
            var configFuture = admin.describeConfigs(resources).all();
            var latestFuture = admin.listOffsets(offsetRequests).all();
            var mainOffsets = admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata();
            var archiveOffsets = admin.listConsumerGroupOffsets(group + ".dead-letter-store").partitionsToOffsetAndMetadata();
            var configs = configFuture.get(5, TimeUnit.SECONDS);
            for (String name : KafkaTopics.ALL) {
                TopicDescription topic = described.get(name);
                Config config = configs.get(new ConfigResource(ConfigResource.Type.TOPIC, name));
                int desired = name.endsWith(".DLT") ? properties.getDeadLetterPartitions()
                        : name.equals(KafkaTopics.CHAT) ? properties.getChatPartitions() : properties.getNotificationPartitions();
                int replicas = topic.partitions().stream().mapToInt(p -> p.replicas().size()).min().orElse(0);
                long underReplicated = topic.partitions().stream().filter(p -> p.isr().size() < p.replicas().size()).count();
                topics.add(new TopicHealth(name, topic.partitions().size(), desired, replicas, value(config, "min.insync.replicas"),
                        value(config, "retention.ms"), value(config, "cleanup.policy"), underReplicated));
                if (underReplicated > 0) warnings.add(name + ": 복제가 지연되는 파티션이 있습니다.");
                if (topic.partitions().size() != desired || replicas != properties.getReplicationFactor()) warnings.add(name + ": 운영 토픽 구성과 신규 생성 설정이 다릅니다. 기존 구성은 자동 변경하지 않습니다.");
                long retention = TimeUnit.DAYS.toMillis(name.endsWith(".DLT") ? properties.getDeadLetterRetentionDays() : properties.getRetentionDays());
                if (!"delete".equals(value(config, "cleanup.policy")) || !Long.toString(retention).equals(value(config, "retention.ms"))
                        || !Integer.toString(properties.getMinInSyncReplicas()).equals(value(config, "min.insync.replicas")))
                    warnings.add(name + ": 보관/복제 정책이 설정값과 다릅니다.");
            }
            Map<TopicPartition, OffsetAndMetadata> committed = new HashMap<>(mainOffsets.get(5, TimeUnit.SECONDS));
            committed.putAll(archiveOffsets.get(5, TimeUnit.SECONDS));
            var ends = latestFuture.get(5, TimeUnit.SECONDS);
            long sum = 0; boolean complete = true;
            for (var tp : offsetRequests.keySet().stream().sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition)).toList()) {
                OffsetAndMetadata offset = committed.get(tp);
                long end = ends.get(tp).offset();
                Long lag = offset == null ? (end == 0 ? Long.valueOf(0) : null) : (offset.offset() > end ? null : Long.valueOf(end - offset.offset()));
                lags.add(new PartitionLag(tp.topic(), tp.partition(), tp.topic().endsWith(".DLT") ? group + ".dead-letter-store" : group,
                        offset == null ? null : offset.offset(), end, lag));
                if (lag == null) { complete = false; warnings.add(tp + ": 커밋된 소비 위치가 없어 지연을 계산할 수 없습니다."); }
                else sum += lag;
            }
            if (complete) totalLag = sum;
            if (sum >= properties.getLagWarningThreshold()) warnings.add("Kafka 소비 지연이 " + sum + "건입니다.");
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            error = "Kafka 상태 조회에 실패했습니다. 브로커 연결·토픽 존재 여부·조회 권한을 확인해 주세요.";
            warnings.add(error);
        }
        cached = new BrokerSnapshot(Instant.now(), error, topics, lags, totalLag, warnings);
        cacheExpires = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        return cached;
    }
    private static String value(Config config, String key) { return config == null || config.get(key) == null ? "unknown" : config.get(key).value(); }
    public record TopicHealth(String topic, int partitions, int configuredPartitions, int replicas, String minInSyncReplicas, String retentionMs, String cleanupPolicy, long underReplicatedPartitions) {}
    public record PartitionLag(String topic, int partition, String group, Long committedOffset, long endOffset, Long lag) {}
    private record BrokerSnapshot(Instant observedAt, String error, List<TopicHealth> topics, List<PartitionLag> lags, Long totalLag, List<String> warnings) {}
    public record Overview(Instant observedAt, String brokerError, List<TopicHealth> topics, List<PartitionLag> lags, Long totalLag,
                           long outboxPending, long oldestOutboxSeconds, long outboxFailed, long unresolvedDeadLetters, long replayPending, List<String> warnings) {}
}
