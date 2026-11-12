package spring.study.kafka.repository;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.dto.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository
public class KafkaClusterRepository {
    // Metadata organization references Provectus Kafka UI; see docs/kafka-ui-reference.md.
    private static final List<String> VISIBLE_CONFIGS = List.of("cleanup.policy", "retention.ms", "retention.bytes", "min.insync.replicas", "max.message.bytes", "segment.bytes");
    private static final String QUERY_ERROR = "Kafka 메타데이터 일부를 조회하지 못했습니다. 연결·토픽·조회 권한을 확인해 주세요.";
    private final Admin admin;
    private final String group;
    private KafkaBrokerSnapshot cached;
    private long cacheExpires;

    public KafkaClusterRepository(Admin admin, @Value("${group-id}") String group) {
        this.admin = admin;
        this.group = group;
    }

    public synchronized KafkaBrokerSnapshot snapshot() {
        if (cached != null && System.nanoTime() < cacheExpires)
            return cached;

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);

        try {
            cached = load(deadline);
        } catch (RuntimeException exception) {
            cached = new KafkaBrokerSnapshot(Instant.now(), QUERY_ERROR, null, null,
                    List.of(), List.of(), List.of(), List.of(), null, List.of(QUERY_ERROR));
        }
        cacheExpires = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        return cached;
    }

    private KafkaBrokerSnapshot load(long deadline) {
        List<String> warnings = new ArrayList<>();
        List<String> groupIds = List.of(group, group + ".dead-letter-store");
        DescribeClusterResult cluster = admin.describeCluster();
        Map<String, KafkaFuture<TopicDescription>>  descriptions = admin.describeTopics(KafkaTopics.ALL).topicNameValues();
        List<ConfigResource> resources = KafkaTopics.ALL.stream().map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name)).toList();
        Map<ConfigResource, KafkaFuture<Config>> configs = admin.describeConfigs(resources).values();
        Map<String, KafkaFuture<ConsumerGroupDescription>> groups = admin.describeConsumerGroups(groupIds).describedGroups();
        Map<String, KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>> offsetQueries = new LinkedHashMap<>();

        groupIds.forEach(id -> offsetQueries.put(id, admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata()));

        Map<String, TopicDescription> topics = new LinkedHashMap<>();

        for (String name : KafkaTopics.ALL) {
            TopicDescription topic = await(descriptions.get(name), deadline, name + ": 토픽 정보를 조회할 수 없습니다.", warnings);
            if (topic != null) topics.put(name, topic);
        }

        Map<TopicPartition, OffsetSpec> earliest = new HashMap<>();
        Map<TopicPartition, OffsetSpec> latest = new HashMap<>();

        topics.values().forEach(topic -> topic.partitions().forEach(partition -> {
            TopicPartition key = new TopicPartition(topic.name(), partition.partition());
            earliest.put(key, OffsetSpec.earliest());
            latest.put(key, OffsetSpec.latest());
        }));

        KafkaFuture<Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo>> startsQuery = earliest.isEmpty() ? null : admin.listOffsets(earliest).all();
        KafkaFuture<Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo>> endsQuery = latest.isEmpty() ? null : admin.listOffsets(latest).all();
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> starts = earliest.isEmpty() ? Map.of() : await(startsQuery, deadline, "시작 오프셋을 조회할 수 없습니다.", warnings);
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> ends = latest.isEmpty() ? Map.of() : await(endsQuery, deadline, "최신 오프셋을 조회할 수 없습니다.", warnings);

        String clusterId = await(cluster.clusterId(), deadline, "클러스터 ID를 조회할 수 없습니다.", warnings);
        Collection<Node> nodes = await(cluster.nodes(), deadline, "브로커 목록을 조회할 수 없습니다.", warnings);
        Node controller = await(cluster.controller(), deadline, "컨트롤러를 조회할 수 없습니다.", warnings);
        Integer controllerId = controller == null || controller.id() < 0 ? null : controller.id();
        List<KafkaTopicSnapshot> snapshots = new ArrayList<>();

        for (String name : KafkaTopics.ALL) {
            TopicDescription topic = topics.get(name);

            if (topic == null) {
                snapshots.add(new KafkaTopicSnapshot(name, List.of(), Map.of(), "토픽 정보를 조회할 수 없습니다."));
                continue;
            }

            Config config = await(configs.get(new ConfigResource(ConfigResource.Type.TOPIC, name)), deadline, name + ": 보관 정책을 조회할 수 없습니다.", warnings);
            Map<String, String> visible = new LinkedHashMap<>();

            if (config != null) {
                for (String key : VISIBLE_CONFIGS) {
                    ConfigEntry entry = config.get(key);

                    if (entry != null && !entry.isSensitive() && entry.value() != null)
                        visible.put(key, entry.value());
                }
            }

            List<KafkaPartitionHealth> partitions = topic.partitions().stream()
                    .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                    .map(partition -> {
                        TopicPartition key = new TopicPartition(name, partition.partition());
                        Node leader = partition.leader();
                        return new KafkaPartitionHealth(partition.partition(), leader == null || leader.id() < 0 ? null : leader.id(),
                                partition.replicas().stream().map(Node::id).toList(),
                                partition.isr().stream().map(Node::id).toList(), offset(starts, key), offset(ends, key));
                    }).toList();

            snapshots.add(new KafkaTopicSnapshot(name, partitions, Map.copyOf(visible),
                    config == null ? "토픽 설정을 조회할 수 없습니다." : null));
        }

        List<KafkaBrokerInfo> brokers = nodes == null ? List.of() : nodes.stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(node -> new KafkaBrokerInfo(node.id(), node.host(), node.port(), node.rack(),
                        Objects.equals(controllerId, node.id()),
                        snapshots.stream().flatMap(topic -> topic.partitions().stream()).filter(p -> Objects.equals(p.leader(), node.id())).count(),
                        snapshots.stream().flatMap(topic -> topic.partitions().stream()).filter(p -> p.replicas().contains(node.id())).count()))
                .toList();

        List<KafkaConsumerGroupInfo> consumers = new ArrayList<>();

        for (String id : groupIds) {
            ConsumerGroupDescription description = await(groups.get(id), deadline, id + ": 그룹 상태를 조회할 수 없습니다.", warnings);
            Map<TopicPartition, OffsetAndMetadata> committed = await(offsetQueries.get(id), deadline, id + ": 소비 위치를 조회할 수 없습니다.", warnings);

            consumers.add(consumer(id, description, committed, snapshots, warnings));
        }

        List<KafkaPartitionLag> lags = consumers.stream().flatMap(consumer -> consumer.lags().stream()).toList();
        boolean complete = topics.size() == KafkaTopics.ALL.size() && !lags.isEmpty() && lags.stream().allMatch(lag -> lag.lag() != null);
        Long totalLag = complete ? lags.stream().mapToLong(KafkaPartitionLag::lag).sum() : null;
        String error = nodes == null || topics.size() != KafkaTopics.ALL.size() ? QUERY_ERROR : null;

        return new KafkaBrokerSnapshot(Instant.now(), error, clusterId, controllerId, brokers, List.copyOf(snapshots), List.copyOf(consumers), lags, totalLag, List.copyOf(warnings));
    }

    private KafkaConsumerGroupInfo consumer(String id, ConsumerGroupDescription description, Map<TopicPartition, OffsetAndMetadata> committed, List<KafkaTopicSnapshot> topics, List<String> warnings) {
        List<KafkaConsumerMemberInfo> members = description == null ? List.of() : description.members().stream()
                .sorted(Comparator.comparing(MemberDescription::consumerId))
                .map(member -> new KafkaConsumerMemberInfo(member.consumerId(), member.clientId(), member.host(),
                        member.assignment().topicPartitions().stream().filter(key -> KafkaTopics.ALL.contains(key.topic()))
                                .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                                .map(key -> key.topic() + " / " + key.partition()).toList()))
                .toList();
        Set<TopicPartition> assigned = new HashSet<>();

        if (description != null)
            description.members().forEach(member -> assigned.addAll(member.assignment().topicPartitions()));

        List<KafkaPartitionLag> lags = new ArrayList<>();

        for (KafkaTopicSnapshot topic : topics) {
            for (KafkaPartitionHealth partition : topic.partitions()) {
                TopicPartition key = new TopicPartition(topic.topic(), partition.partition());
                boolean expected = topic.topic().endsWith(".DLT") == id.equals(group + ".dead-letter-store");

                if (!expected && !assigned.contains(key) && (committed == null || !committed.containsKey(key)))
                    continue;

                OffsetAndMetadata position = committed == null ? null : committed.get(key);
                Long current = position == null ? null : position.offset();
                Long start = partition.startOffset(), end = partition.endOffset(), lag = null;

                if (committed != null && start != null && end != null && start <= end) {
                    if (current == null && start.equals(end))
                        lag = 0L;
                    else if (current != null && current >= start && current <= end)
                        lag = end - current;
                }

                if (lag == null)
                    warnings.add(id + " / " + topic.topic() + " / " + partition.partition() + ": 유효한 소비 위치 또는 오프셋 범위가 없어 지연을 계산할 수 없습니다.");

                lags.add(new KafkaPartitionLag(topic.topic(), partition.partition(), id, current, end, lag));
            }
        }

        boolean complete = topics.stream()
                .filter(topic -> topic.topic().endsWith(".DLT") == id.equals(group + ".dead-letter-store"))
                .noneMatch(topic -> topic.partitions().isEmpty())
                && !lags.isEmpty() && lags.stream().allMatch(lag -> lag.lag() != null);

        Long total = complete ? lags.stream().mapToLong(KafkaPartitionLag::lag).sum() : null;

        return new KafkaConsumerGroupInfo(id, description == null ? "UNKNOWN" : description.state().name(),
                description == null || description.coordinator() == null ? null : description.coordinator().id(),
                description == null ? null : description.partitionAssignor(), description == null ? null : members.size(), total,
                members, List.copyOf(lags), description == null || committed == null ? "소비자 그룹 정보를 일부 조회할 수 없습니다." : null);
    }

    private Long offset(Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets, TopicPartition key) {
        return offsets == null || offsets.get(key) == null ? null : offsets.get(key).offset();
    }

    private <T> T await(KafkaFuture<T> future, long deadline, String warning, List<String> warnings) {
        try {
            return future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException)
                Thread.currentThread().interrupt();

            warnings.add(warning);

            return null;
        }
    }
}
