package spring.study.kafka.component;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import spring.study.kafka.event.KafkaOutboxDispatchRequestedEvent;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.study.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaOutboxDispatchListener {
    private final KafkaOutboxDispatchCoordinator coordinator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void requestDispatch(KafkaOutboxDispatchRequestedEvent event) {
        coordinator.requestImmediateDispatch();
    }
}
