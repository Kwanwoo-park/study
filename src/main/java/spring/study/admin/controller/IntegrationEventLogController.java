package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.dto.IntegrationEventLogResponse;
import spring.study.admin.entity.IntegrationEventLog.*;
import spring.study.admin.service.IntegrationEventLogQueryService;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class IntegrationEventLogController {
    private final IntegrationEventLogQueryService service;

    @GetMapping("/admin/event-logs")
    public String page(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "admin/event_logs";
    }

    @GetMapping("/api/admin/event-logs")
    @ResponseBody
    public ResponseEntity<IntegrationEventLogResponse> list(@RequestParam(required = false) Broker broker,
            @RequestParam(required = false) Operation operation, @RequestParam(required = false) Outcome outcome,
            @RequestParam(required = false) Long beforeId, @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.find(broker, operation, outcome, beforeId, hours));
    }

    @GetMapping("/api/admin/event-logs/{id}")
    @ResponseBody
    public ResponseEntity<IntegrationEventLogResponse.Item> detail(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.findById(id));
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ResponseEntity<?> requestError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("message", error.getReason() == null ? "조회 조건을 확인해 주세요" : error.getReason()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<?> invalidFilter() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message", "조회 조건을 확인해 주세요"));
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseBody
    public ResponseEntity<?> unavailable() {
        return ResponseEntity.status(503).cacheControl(CacheControl.noStore())
                .body(Map.of("message", "이벤트 로그 저장소에 연결하지 못했습니다. DB와 integration_event_log 테이블을 확인해 주세요."));
    }
}
