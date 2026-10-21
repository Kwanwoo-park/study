package spring.study.admin.controller;

import spring.study.admin.facade.IntegrationEventLogFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import spring.study.admin.entity.IntegrationEventLogBroker;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/event-logs")
@PreAuthorize("hasRole('ADMIN')")
public class IntegrationEventLogController {
    private final IntegrationEventLogFacade facade;

    @GetMapping({"", "/"})
    public ResponseEntity<?> list(@RequestParam(required = false) IntegrationEventLogBroker broker,
                                  @RequestParam(required = false) IntegrationEventLogOperation operation,
                                  @RequestParam(required = false) IntegrationEventLogOutcome outcome,
                                  @RequestParam(required = false) Long beforeId,
                                  @RequestParam(defaultValue = "24") int hours) {
        return facade.list(broker, operation, outcome, beforeId, hours);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable long id) {
        return facade.detail(id);
    }
}
