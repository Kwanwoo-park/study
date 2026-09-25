package spring.study.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.entity.IntegrationEventLog;
import spring.study.admin.repository.IntegrationEventLogRepository;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static spring.study.admin.entity.IntegrationEventLog.*;

class IntegrationEventLogQueryServiceTest {
    private final IntegrationEventLogRepository repository = mock(IntegrationEventLogRepository.class);
    private final IntegrationEventLogService recorder = new IntegrationEventLogService(mock(IntegrationEventLogStore.class), true, 10, 7);
    private final IntegrationEventLogQueryService service = new IntegrationEventLogQueryService(repository, recorder);

    @Test
    void usesExtraRowToReturnStableCursorWithoutACountQuery() {
        var logs = IntStream.range(0, 51).mapToObj(i -> {
            var log = new IntegrationEventLog(new Entry(LocalDateTime.now(), "node", Route.CHAT, Operation.PUBLISH, Outcome.SUCCESS, null, 1, null, null, null));
            ReflectionTestUtils.setField(log, "id", 100L - i); return log;
        }).toList();
        when(repository.search(any(), isNull(), isNull(), isNull(), isNull(), any())).thenReturn(logs);
        var response = service.find(null, null, null, null, 24);
        assertThat(response.entries()).hasSize(50);
        assertThat(response.nextBeforeId()).isEqualTo(51L);
    }

    @Test
    void rejectsInvalidCursorsAndPeriodsBeforeQuerying() {
        assertThatThrownBy(() -> service.find(null, null, null, 0L, 24)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.find(null, null, null, null, 721)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.find(null, null, null, null, 0)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(repository);
    }
}
