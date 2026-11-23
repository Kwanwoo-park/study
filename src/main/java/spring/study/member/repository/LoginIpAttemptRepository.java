package spring.study.member.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.member.entity.LoginIpAttempt;

import java.util.Optional;

public interface LoginIpAttemptRepository extends JpaRepository<LoginIpAttempt, String> {
    @Modifying
    @Query(value = """
            INSERT INTO login_ip_attempt (ip_address, failed_attempts)
            VALUES (:address, 0)
            ON DUPLICATE KEY UPDATE ip_address = :address
            """, nativeQuery = true)
    void ensureCounter(@Param("address") String address);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from LoginIpAttempt attempt where attempt.ipAddress = :address")
    Optional<LoginIpAttempt> findCounterForUpdate(@Param("address") String address);
}
