package spring.study.mail.component;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.common.component.TransactionRunner;
import spring.study.mail.entity.EmailVerificationAttempt;
import spring.study.mail.repository.EmailVerificationAttemptRepository;

import java.sql.Timestamp;
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
    private static final String EMAIL = "blocked@example.test";

    @Autowired EmailVerificationGuard guard;
    @Autowired EmailVerificationAttemptRepository attempts;
    @Autowired TransactionRunner transactions;
    @Autowired JdbcTemplate jdbc;

    @Test
    void usesARollingDayAndKeepsTheCommittedBlock() {
        String emailDigest = "a".repeat(64);
        Instant now = Instant.now();
        transactions.write(() -> {
            attempts.ensureCounter(emailDigest);
            var attempt = attempts.findCounterForUpdate(emailDigest).orElseThrow();
            assertThat(attempt.allowRequest(now.minus(Duration.ofHours(25)), EMAIL)).isTrue();
            assertThat(attempt.allowRequest(now.minus(Duration.ofHours(23)), EMAIL)).isTrue();
            return null;
        });

        guard.recordRequest(emailDigest, EMAIL);
        assertThat(guard.isBlocked(emailDigest)).isFalse();
        assertThat(attempts.findById(emailDigest).orElseThrow().getBlockedEmail()).isNull();

        assertThatThrownBy(() -> guard.recordRequest(emailDigest, EMAIL))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        var originalBlock = attempts.findById(emailDigest).orElseThrow();
        assertThat(originalBlock.getBlockedAt()).isNotNull();
        assertThat(originalBlock.getBlockedEmail()).isEqualTo(EMAIL);

        var newGuard = new EmailVerificationGuard(attempts, transactions);
        assertThatThrownBy(() -> newGuard.requireAllowed(emailDigest))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> newGuard.recordRequest(emailDigest, "Blocked@example.test"))
                .isInstanceOf(ResponseStatusException.class);
        var savedBlock = attempts.findById(emailDigest).orElseThrow();
        assertThat(savedBlock.getBlockedAt()).isEqualTo(originalBlock.getBlockedAt());
        assertThat(savedBlock.getBlockedEmail()).isEqualTo(EMAIL);
    }

    @Test
    void listsOnlyBlocksNewestFirstAndFillsTheAddressOfALegacyBlock() {
        String legacyDigest = "c".repeat(64);
        String pendingDigest = "d".repeat(64);
        String currentDigest = "e".repeat(64);
        Instant legacyBlockedAt = Instant.now().minus(Duration.ofDays(2));
        jdbc.update("INSERT INTO email_verification_attempt (email_digest, blocked_at) VALUES (?, ?)",
                legacyDigest, Timestamp.from(legacyBlockedAt));
        var legacyBlock = attempts.findById(legacyDigest).orElseThrow();
        assertThat(legacyBlock.getBlockedEmail()).isNull();

        guard.recordRequest(pendingDigest, "pending@example.test");
        guard.recordRequest(currentDigest, EMAIL);
        guard.recordRequest(currentDigest, EMAIL);
        assertThatThrownBy(() -> guard.recordRequest(currentDigest, EMAIL)).isInstanceOf(ResponseStatusException.class);
        assertThat(guard.listBlocked()).extracting(EmailVerificationAttempt::getEmailDigest)
                .containsSubsequence(currentDigest, legacyDigest).doesNotContain(pendingDigest);

        assertThatThrownBy(() -> guard.recordRequest(legacyDigest, "legacy@example.test"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        var updatedBlock = attempts.findById(legacyDigest).orElseThrow();
        assertThat(updatedBlock.getBlockedEmail()).isEqualTo("legacy@example.test");
        assertThat(updatedBlock.getBlockedAt()).isEqualTo(legacyBlock.getBlockedAt());
    }

    @Test
    void manualBlockIsImmediateAndRepeatedRegistrationKeepsTheOriginalRecord() {
        String emailDigest = "f".repeat(64);

        guard.block(emailDigest, EMAIL);
        var original = attempts.findById(emailDigest).orElseThrow();
        assertThat(original.getBlockedEmail()).isEqualTo(EMAIL);
        assertThat(original.getBlockedAt()).isNotNull();
        assertThat(original.getLastRequestAt()).isNull();
        assertThat(guard.isBlocked(emailDigest)).isTrue();

        guard.block(emailDigest, "Blocked@example.test");
        assertThatThrownBy(() -> guard.recordRequest(emailDigest, EMAIL))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        var saved = attempts.findById(emailDigest).orElseThrow();
        assertThat(saved.getBlockedAt()).isEqualTo(original.getBlockedAt());
        assertThat(saved.getBlockedEmail()).isEqualTo(EMAIL);
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
                        guard.recordRequest(emailDigest, EMAIL);
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
