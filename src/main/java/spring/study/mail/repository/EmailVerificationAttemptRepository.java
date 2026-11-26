package spring.study.mail.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.mail.entity.EmailVerificationAttempt;

import java.util.Optional;

public interface EmailVerificationAttemptRepository extends JpaRepository<EmailVerificationAttempt, String> {
    @Modifying
    @Query(value = """
            INSERT INTO email_verification_attempt (email_digest)
            VALUES (:emailDigest)
            ON DUPLICATE KEY UPDATE email_digest = :emailDigest
            """, nativeQuery = true)
    void ensureCounter(@Param("emailDigest") String emailDigest);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from EmailVerificationAttempt attempt where attempt.emailDigest = :emailDigest")
    Optional<EmailVerificationAttempt> findCounterForUpdate(@Param("emailDigest") String emailDigest);

    boolean existsByEmailDigestAndBlockedAtIsNotNull(String emailDigest);
}
