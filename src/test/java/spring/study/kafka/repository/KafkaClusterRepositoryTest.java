package spring.study.kafka.repository;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spring.study.kafka.config.KafkaTopics;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class KafkaClusterRepositoryTest {
    private final Admin admin = mock(Admin.class);
    private final Node broker = new Node(1, "broker.test", 9092);
    private final Map<String, KafkaFuture<TopicDescription>> descriptions = new HashMap<>();
    private final Map<String, Map<TopicPartition, OffsetAndMetadata>> offsets = new HashMap<>();
    private final Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> starts = new HashMap<>();
    private final Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> ends = new HashMap<>();
    private KafkaClusterRepository repository;

    @BeforeEach
    void setUp() {
        repository = new KafkaClusterRepository(admin, "study");
        DescribeClusterResult cluster = mock(DescribeClusterResult.class);
        when(admin.describeCluster()).thenReturn(cluster);
        when(cluster.clusterId()).thenReturn(KafkaFuture.completedFuture("cluster-test"));
        when(cluster.nodes()).thenReturn(KafkaFuture.completedFuture(List.of(broker)));
        when(cluster.controller()).thenReturn(KafkaFuture.completedFuture(broker));
        Map<ConfigResource, KafkaFuture<Config>> configs = new HashMap<>();
        for (String name : KafkaTopics.ALL) {
            TopicPartition key = new TopicPartition(name, 0);
            descriptions.put(name, KafkaFuture.completedFuture(new TopicDescription(name, false,
                    List.of(new TopicPartitionInfo(0, broker, List.of(broker), List.of(broker))))));
            configs.put(new ConfigResource(ConfigResource.Type.TOPIC, name), KafkaFuture.completedFuture(new Config(List.of(
                    new ConfigEntry("cleanup.policy", "delete"), new ConfigEntry("retention.ms", "604800000"),
                    new ConfigEntry("min.insync.replicas", "1"), new ConfigEntry("sasl.jaas.config", "private-secret")))));
            starts.put(key, info(0));
            ends.put(key, info(10));
        }
        DescribeTopicsResult topics = mock(DescribeTopicsResult.class);
        when(admin.describeTopics(KafkaTopics.ALL)).thenReturn(topics);
        when(topics.topicNameValues()).thenReturn(descriptions);
        DescribeConfigsResult configResult = mock(DescribeConfigsResult.class);
        when(admin.describeConfigs(anyCollection())).thenReturn(configResult);
        when(configResult.values()).thenReturn(configs);
        Map<String, KafkaFuture<ConsumerGroupDescription>> groups = new HashMap<>();
        for (String id : List.of("study", "study.dead-letter-store")) {
            Set<TopicPartition> assignments = new HashSet<>();
            Map<TopicPartition, OffsetAndMetadata> positions = new HashMap<>();
            KafkaTopics.ALL.stream().filter(name -> name.endsWith(".DLT") == id.endsWith(".dead-letter-store"))
                    .forEach(name -> { var key = new TopicPartition(name, 0); assignments.add(key); positions.put(key, new OffsetAndMetadata(8)); });
            offsets.put(id, positions);
            var member = new MemberDescription("consumer-" + id, "client-" + id, "/127.0.0.1", new MemberAssignment(assignments));
            groups.put(id, KafkaFuture.completedFuture(new ConsumerGroupDescription(
                    id, false, List.of(member), "range", ConsumerGroupState.STABLE, broker)));
            ListConsumerGroupOffsetsResult result = mock(ListConsumerGroupOffsetsResult.class);
            when(admin.listConsumerGroupOffsets(id)).thenReturn(result);
            when(result.partitionsToOffsetAndMetadata()).thenAnswer(ignored -> KafkaFuture.completedFuture(offsets.get(id)));
        }
        when(admin.describeConsumerGroups(List.of("study", "study.dead-letter-store")))
                .thenReturn(new DescribeConsumerGroupsResult(groups));
        when(admin.listOffsets(anyMap())).thenAnswer(invocation -> {
            Map<TopicPartition, OffsetSpec> request = invocation.getArgument(0);
            boolean earliest = request.values().iterator().next().getClass().equals(OffsetSpec.earliest().getClass());
            ListOffsetsResult result = mock(ListOffsetsResult.class);
            when(result.all()).thenReturn(KafkaFuture.completedFuture(earliest ? starts : ends));
            return result;
        });
    }

    @Test
    void collectsBrokersPartitionsAndEachConsumerGroupWithoutExposingArbitraryConfigs() {
        var result = repository.snapshot();

        assertThat(result.error()).isNull();
        assertThat(result.clusterId()).isEqualTo("cluster-test");
        assertThat(result.brokers()).singleElement().satisfies(node -> {
            assertThat(node.controller()).isTrue();
            assertThat(node.leaderPartitions()).isEqualTo(4);
            assertThat(node.replicaPartitions()).isEqualTo(4);
        });
        assertThat(result.topics()).hasSize(4).allSatisfy(topic -> {
            assertThat(topic.configs()).doesNotContainKey("sasl.jaas.config");
            assertThat(topic.partitions()).singleElement().satisfies(p -> {
                assertThat(p.startOffset()).isZero();
                assertThat(p.endOffset()).isEqualTo(10);
            });
        });
        assertThat(result.consumerGroups()).hasSize(2).allSatisfy(group -> {
            assertThat(group.state()).isEqualTo("STABLE");
            assertThat(group.memberCount()).isEqualTo(1);
            assertThat(group.members().get(0).assignments()).hasSize(2);
            assertThat(group.totalLag()).isEqualTo(4);
        });
        assertThat(result.totalLag()).isEqualTo(8);
        assertThat(repository.snapshot()).isSameAs(result);
        verify(admin, times(1)).describeCluster();
    }

    @Test
    void anotherGroupsCommittedOffsetMustNotOverwriteTheMainGroup() {
        offsets.get("study.dead-letter-store").put(new TopicPartition(KafkaTopics.CHAT, 0), new OffsetAndMetadata(9));

        assertThat(repository.snapshot().consumerGroups().get(0).lags())
                .filteredOn(lag -> lag.topic().equals(KafkaTopics.CHAT)).singleElement()
                .satisfies(lag -> assertThat(lag.lag()).isEqualTo(2));
    }

    @Test
    void unknownOrOutOfRetentionRangeOffsetsMustNotBecomeZeroLag() {
        offsets.get("study").remove(new TopicPartition(KafkaTopics.CHAT, 0));
        starts.put(new TopicPartition(KafkaTopics.NOTIFICATION, 0), info(9));

        var result = repository.snapshot();

        assertThat(result.totalLag()).isNull();
        assertThat(result.consumerGroups().get(0).lags()).allSatisfy(lag -> assertThat(lag.lag()).isNull());
    }

    @Test
    void oneUnavailableTopicDoesNotHideHealthySectionsAndErrorsAreSanitized() {
        KafkaFutureImpl<TopicDescription> failed = new KafkaFutureImpl<>();
        failed.completeExceptionally(new IllegalStateException("private-secret-connection-details"));
        descriptions.put(KafkaTopics.CHAT_DLT, failed);

        var result = repository.snapshot();

        assertThat(result.error()).isNotNull().doesNotContain("private-secret");
        assertThat(result.brokers()).hasSize(1);
        assertThat(result.topics()).filteredOn(topic -> topic.topic().equals(KafkaTopics.CHAT)).singleElement()
                .satisfies(topic -> assertThat(topic.partitions()).hasSize(1));
        assertThat(result.consumerGroups().get(0).totalLag()).isEqualTo(4);
        assertThat(result.totalLag()).isNull();
        assertThat(result.warnings()).noneMatch(warning -> warning.contains("private-secret"));
    }

    private ListOffsetsResult.ListOffsetsResultInfo info(long offset) {
        return new ListOffsetsResult.ListOffsetsResultInfo(offset, -1, Optional.empty());
    }
}
