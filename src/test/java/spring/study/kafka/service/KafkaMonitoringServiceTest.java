package spring.study.kafka.service;

import org.apache.kafka.clients.admin.Admin;
import org.junit.jupiter.api.Test;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.kafka.repository.KafkaClusterRepository;
import spring.study.kafka.dto.KafkaBrokerSnapshot;
import spring.study.kafka.dto.KafkaPartitionHealth;
import spring.study.kafka.dto.KafkaTopicSnapshot;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class KafkaMonitoringServiceTest {
    @Test void brokerOutageIsUnknownNotZeroAndRepeatedRequestsUseShortCache() {
        Admin admin = mock(Admin.class);
        var outbox = mock(KafkaOutboxEventRepository.class);
        var deadLetters = mock(KafkaDeadLetterRepository.class);
        when(admin.describeCluster()).thenThrow(new IllegalStateException("private-connection-info"));
        when(outbox.findOldestPendingTime()).thenReturn(LocalDateTime.now().minusMinutes(2));
        var service = new KafkaMonitoringService(new KafkaClusterRepository(admin, "test"), new KafkaOperationsProperties(), outbox, deadLetters);
        var result = service.overview(); service.overview();
        assertThat(result.totalLag()).isNull();
        assertThat(result.brokerError()).doesNotContain("private-connection-info");
        assertThat(result.oldestOutboxSeconds()).isGreaterThanOrEqualTo(120);
        assertThat(result.warnings()).hasSize(2);
        verify(admin, times(1)).describeCluster();
    }

    @Test void warnsAboutLeaderlessAndUnderReplicatedPartitionsWithoutChangingKafka() {
        var cluster = mock(KafkaClusterRepository.class);
        var partition = new KafkaPartitionHealth(0, null, List.of(1, 2), List.of(1), 0L, 100L);
        var topic = new KafkaTopicSnapshot("topic", List.of(partition), Map.of(
                "cleanup.policy", "delete", "retention.ms", "604800000", "min.insync.replicas", "1"), null);
        when(cluster.snapshot()).thenReturn(new KafkaBrokerSnapshot(Instant.now(), null, "cluster", 1,
                List.of(), List.of(topic), List.of(), List.of(), 101L, List.of()));
        var service = new KafkaMonitoringService(cluster, new KafkaOperationsProperties(),
                mock(KafkaOutboxEventRepository.class), mock(KafkaDeadLetterRepository.class));

        var result = service.overview();

        assertThat(result.warnings()).anyMatch(warning -> warning.contains("리더가 없는"));
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("복제가 지연"));
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("소비 지연이 101건"));
        verify(cluster).snapshot();
        verifyNoMoreInteractions(cluster);
    }
}
