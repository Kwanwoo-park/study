package spring.study.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.dto.IntegrationEventLogResponse;
import spring.study.admin.dto.IntegrationEventLogResponse.Item;
import spring.study.admin.entity.IntegrationEventLog;
import spring.study.admin.entity.IntegrationEventLog.*;
import spring.study.admin.repository.IntegrationEventLogRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegrationEventLogQueryService {
    private static final int PAGE_SIZE = 50;
    private final IntegrationEventLogRepository repository;
    private final IntegrationEventLogService recorder;

    @Transactional(readOnly = true)
    public IntegrationEventLogResponse find(Broker broker, Operation operation, Outcome outcome, Long beforeId, int hours) {
        if ((beforeId != null && beforeId < 1) || hours < 1 || hours > 720) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 기간과 페이지를 확인해 주세요");
        }
        LocalDateTime since = LocalDateTime.now().minusHours(Math.min(hours, recorder.retentionDays() * 24L));
        List<IntegrationEventLog> logs = repository.search(since, broker, operation, outcome, beforeId, PageRequest.of(0, PAGE_SIZE + 1));
        List<Item> entries = logs.stream().limit(PAGE_SIZE).map(Item::new).toList();
        Long next = logs.size() > PAGE_SIZE ? entries.get(entries.size() - 1).id() : null;
        return new IntegrationEventLogResponse(entries, next, recorder.retentionDays(), recorder.diagnostics());
    }
}
