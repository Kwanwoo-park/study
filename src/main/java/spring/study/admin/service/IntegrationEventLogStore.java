package spring.study.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.study.admin.entity.IntegrationEventLog;
import spring.study.admin.entity.IntegrationEventLog.Entry;
import spring.study.admin.repository.IntegrationEventLogRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegrationEventLogStore {
    private final IntegrationEventLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(List<Entry> entries) {
        // New entities on each attempt: a rolled-back flush may have assigned IDs.
        repository.saveAllAndFlush(entries.stream().map(IntegrationEventLog::new).toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteExpiredBatch(LocalDateTime cutoff) {
        List<Long> ids = repository.findExpiredIds(cutoff, PageRequest.of(0, 1000));
        if (!ids.isEmpty()) repository.deleteAllByIdInBatch(ids);
        return ids.size();
    }
}
