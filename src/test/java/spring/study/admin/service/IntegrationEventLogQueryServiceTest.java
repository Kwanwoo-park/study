package spring.study.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.entity.IntegrationEventLog;
import spring.study.admin.repository.IntegrationEventLogRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
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

    @Test
    void detailReturnsStoredMetadataWithoutDiscardingZeroCounts() {
        var log = new IntegrationEventLog(new Entry(LocalDateTime.now(), "node-1", Route.REALTIME_NOTIFICATION,
                Operation.PUBLISH, Outcome.NO_SUBSCRIBERS, 42L, 1, null, 0L, null));
        ReflectionTestUtils.setField(log, "id", 15L);
        when(repository.findById(15L)).thenReturn(Optional.of(log));
        var detail = service.findById(15L);
        assertThat(detail.id()).isEqualTo(15L);
        assertThat(detail.destination()).isEqualTo("notification-events");
        assertThat(detail.referenceId()).isEqualTo(42L);
        assertThat(detail.subscriberCount()).isZero();
        assertThat(detail.attempt()).isNull();
        assertThat(detail.instanceId()).isEqualTo("node-1");
    }

    @Test
    void missingAndExpiredDetailsReturnNotFound() {
        when(repository.findById(12L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(12L)).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        var expired = new IntegrationEventLog(new Entry(LocalDateTime.now().minusDays(8), "node", Route.CHAT,
                Operation.CONSUME, Outcome.SUCCESS, null, 1, null, null, null));
        when(repository.findById(13L)).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.findById(13L)).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void invalidDetailIdDoesNotQueryDatabase() {
        assertThatThrownBy(() -> service.findById(0)).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(repository);
    }
}
