package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import spring.study.kafka.entity.KafkaDeadLetter.Status;
import spring.study.kafka.service.KafkaDeadLetterService;
import spring.study.kafka.service.KafkaMonitoringService;
import spring.study.member.entity.Member;
import java.util.Map;

@Controller @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class KafkaOperationsController {
    private final KafkaMonitoringService monitoring;
    private final KafkaDeadLetterService deadLetters;
    @GetMapping("/admin/kafka")
    public String page(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store"); return "admin/kafka_operations";
    }
    @GetMapping("/api/admin/kafka/overview") @ResponseBody
    public ResponseEntity<?> overview() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(monitoring.overview()); }
    @GetMapping("/api/admin/kafka/dead-letters") @ResponseBody
    public ResponseEntity<?> list(@RequestParam(required = false) Status status, @RequestParam(required = false) Long beforeId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(deadLetters.list(status, beforeId));
    }
    @GetMapping("/api/admin/kafka/csrf") @ResponseBody
    public ResponseEntity<?> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }
    @PostMapping("/api/admin/kafka/dead-letters/{id}/replay") @ResponseBody
    public ResponseEntity<?> replay(@PathVariable long id, @AuthenticationPrincipal Member administrator) {
        deadLetters.requestReplay(id, administrator.getId());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("message", "재발행 요청을 저장했습니다. 처리 상태를 확인해 주세요."));
    }
    @ExceptionHandler(ResponseStatusException.class) @ResponseBody
    public ResponseEntity<?> requestError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("message", error.getReason() == null ? "요청을 확인해 주세요." : error.getReason()));
    }
    @ExceptionHandler(DataAccessException.class) @ResponseBody
    public ResponseEntity<?> databaseError() {
        return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).body(Map.of("message", "Kafka 운영 기록을 조회하지 못했습니다. DB 연결과 테이블을 확인해 주세요."));
    }
}
