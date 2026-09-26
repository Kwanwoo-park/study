package spring.study.kafka.service;

import org.apache.kafka.clients.admin.Admin;
import org.junit.jupiter.api.Test;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class KafkaMonitoringServiceTest {
    @Test void brokerOutageIsUnknownNotZeroAndRepeatedRequestsUseShortCache() {
        Admin admin = mock(Admin.class);
        var outbox = mock(KafkaOutboxEventRepository.class);
        var deadLetters = mock(KafkaDeadLetterRepository.class);
        when(admin.describeTopics(KafkaTopics.ALL)).thenThrow(new IllegalStateException("private-connection-info"));
        when(outbox.findOldestPendingTime()).thenReturn(LocalDateTime.now().minusMinutes(2));
        var service = new KafkaMonitoringService(admin, new KafkaOperationsProperties(), outbox, deadLetters, "test");
        var result = service.overview(); service.overview();
        assertThat(result.totalLag()).isNull();
        assertThat(result.brokerError()).doesNotContain("private-connection-info");
        assertThat(result.oldestOutboxSeconds()).isGreaterThanOrEqualTo(120);
        assertThat(result.warnings()).hasSize(2);
        verify(admin, times(1)).describeTopics(KafkaTopics.ALL);
    }
}
