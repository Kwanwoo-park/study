package spring.study.admin.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import spring.study.admin.entity.IntegrationEventLogBroker;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import org.springframework.stereotype.Service;
import spring.study.admin.service.IntegrationEventLogService;

@Service
@RequiredArgsConstructor
public class IntegrationEventLogFacade {
    private final IntegrationEventLogService integrationEventLogService;

    public ResponseEntity<?> list(IntegrationEventLogBroker broker, IntegrationEventLogOperation operation, IntegrationEventLogOutcome outcome, Long beforeId, int hours) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(integrationEventLogService.find(broker, operation, outcome, beforeId, hours));
    }

    public ResponseEntity<?> detail(long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(integrationEventLogService.findById(id));
    }
}
