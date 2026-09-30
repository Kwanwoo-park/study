package spring.study.admin.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.admin.dto.IntegrationEventLogResponse;
import spring.study.admin.entity.IntegrationEventLog.*;
import spring.study.admin.service.IntegrationEventLogQueryService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntegrationEventLogFacade {
    private final IntegrationEventLogQueryService service;

    public ResponseEntity<?> list(Broker broker, Operation operation, Outcome outcome, Long beforeId, int hours) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.find(broker, operation, outcome, beforeId, hours));
    }

    public ResponseEntity<?> detail(long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.findById(id));
    }
}
