package spring.study.admin.controller;

import spring.study.kafka.facade.KafkaOperationsFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.member.entity.Member;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/kafka")
@PreAuthorize("hasRole('ADMIN')")
public class KafkaOperationsController {
    private final KafkaOperationsFacade facade;

    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        return facade.overview();
    }

    @GetMapping("/dead-letters")
    public ResponseEntity<?> list(@RequestParam(required = false) KafkaDeadLetterStatus status, @RequestParam(required = false) Long beforeId) {
        return facade.list(status, beforeId);
    }

    @GetMapping("/csrf")
    public ResponseEntity<?> csrf(CsrfToken token) {
        return facade.csrf(token);
    }

    @PostMapping("/dead-letters/{id}/replay")
    public ResponseEntity<?> replay(@PathVariable long id, @AuthenticationPrincipal Member administrator) {
        return facade.replay(id, administrator);
    }
}
