package spring.study.mail.component;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.common.component.TransactionRunner;
import spring.study.mail.repository.EmailVerificationAttemptRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:email-verification-attempt-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EmailVerificationGuard.class, TransactionRunner.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailVerificationGuardPersistenceTest {
    @Autowired EmailVerificationGuard guard;
    @Autowired EmailVerificationAttemptRepository attempts;
    @Autowired TransactionRunner transactions;

    @Test
    void usesARollingDayAndKeepsTheCommittedBlock() {
        String emailDigest = "a".repeat(64);
        Instant now = Instant.now();
        transactions.write(() -> {
            attempts.ensureCounter(emailDigest);
            var attempt = attempts.findCounterForUpdate(emailDigest).orElseThrow();
            assertThat(attempt.allowRequest(now.minus(Duration.ofHours(25)))).isTrue();
            assertThat(attempt.allowRequest(now.minus(Duration.ofHours(23)))).isTrue();
            return null;
        });

        guard.recordRequest(emailDigest);
        assertThat(guard.isBlocked(emailDigest)).isFalse();

        assertThatThrownBy(() -> guard.recordRequest(emailDigest))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        Instant blockedAt = attempts.findById(emailDigest).orElseThrow().getBlockedAt();
        assertThat(blockedAt).isNotNull();

        var newGuard = new EmailVerificationGuard(attempts, transactions);
        assertThatThrownBy(() -> newGuard.requireAllowed(emailDigest))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> newGuard.recordRequest(emailDigest)).isInstanceOf(ResponseStatusException.class);
        assertThat(attempts.findById(emailDigest).orElseThrow().getBlockedAt()).isEqualTo(blockedAt);
    }

    @Test
    void simultaneousRequestsPermitAtMostTwoForTheSameAddress() throws Exception {
        String emailDigest = "b".repeat(64);
        var workers = Executors.newFixedThreadPool(6);
        var start = new CountDownLatch(1);
        List<Future<HttpStatus>> requests = new ArrayList<>();
        try {
            for (int index = 0; index < 6; index++) {
                requests.add(workers.submit(() -> {
                    start.await();
                    try {
                        guard.recordRequest(emailDigest);
                        return HttpStatus.OK;
                    } catch (ResponseStatusException rejection) {
                        return HttpStatus.valueOf(rejection.getStatusCode().value());
                    }
                }));
            }
            start.countDown();

            List<HttpStatus> statuses = new ArrayList<>();
            for (var request : requests) statuses.add(request.get(10, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.OK,
                    HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN);
            assertThat(guard.isBlocked(emailDigest)).isTrue();
        } finally {
            workers.shutdownNow();
        }
    }
}
