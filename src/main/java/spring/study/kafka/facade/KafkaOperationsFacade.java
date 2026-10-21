package spring.study.kafka.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.kafka.service.KafkaDeadLetterService;
import spring.study.kafka.service.KafkaMonitoringService;
import spring.study.member.entity.Member;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaOperationsFacade {
    private final KafkaMonitoringService monitoring;
    private final KafkaDeadLetterService deadLetters;

    public ResponseEntity<?> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken()
        ));
    }

    public ResponseEntity<?> overview() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(monitoring.overview()); }

    public ResponseEntity<?> list(KafkaDeadLetterStatus status, Long beforeId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(deadLetters.list(status, beforeId));
    }

    public ResponseEntity<?> replay(long id, Member administrator) {
        deadLetters.requestReplay(id, administrator.getId());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("message", "재발행 요청을 저장했습니다. 처리 상태를 확인해 주세요."));
    }
}
