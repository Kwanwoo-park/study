package spring.study.member.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import spring.study.member.entity.AccountStatus;
import spring.study.member.repository.MemberRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberSanctionExpirationService {
    private final MemberRepository memberRepository;

    @Scheduled(fixedDelayString = "${spring.study.sanction-expiration-delay-ms:60000}")
    @Transactional
    public void activateExpiredSuspensions() {
        memberRepository.findByAccountStatusAndSuspendedUntilLessThanEqual(
                AccountStatus.SUSPENDED,
                LocalDateTime.now()
        ).forEach(member -> member.activate());
    }
}
