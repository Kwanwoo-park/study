package spring.study.common.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Executes a complete use case before its facade translates exceptions into HTTP responses. */
@Component
@RequiredArgsConstructor
public class TransactionRunner {
    private final PlatformTransactionManager transactionManager;

    public <T> T write(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
}
