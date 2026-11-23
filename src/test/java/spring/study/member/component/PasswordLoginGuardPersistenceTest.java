package spring.study.member.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.repository.BlockedIpRepository;
import spring.study.admin.service.IpBlockService;
import spring.study.common.component.TransactionRunner;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.LoginIpAttemptRepository;
import spring.study.member.service.MemberService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:login-ip-attempt-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PasswordLoginGuard.class, IpBlockService.class, TransactionRunner.class,
        PasswordLoginGuardPersistenceTest.PasswordConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PasswordLoginGuardPersistenceTest {
    private static final String MEMBER_EMAIL = "member@example.test";
    private static final String MISSING_EMAIL = "missing@example.test";
    private static final String MATCHING_PASSWORD = "fixture-password";

    @Autowired PasswordLoginGuard guard;
    @Autowired LoginIpAttemptRepository attempts;
    @Autowired BlockedIpRepository blocks;
    @Autowired BCryptPasswordEncoder encoder;
    @MockBean MemberService members;
    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(7L).role(Role.USER).email(MEMBER_EMAIL)
                .pwd(encoder.encode(MATCHING_PASSWORD)).build();
        when(members.findMemberOrNull(MEMBER_EMAIL)).thenReturn(member);
    }

    @Test
    void sixthAttemptBlocksPersistentlyEvenWhenPresentedWithCorrectCredentials() {
        String address = "203.0.113.50";
        for (int index = 0; index < 4; index++) {
            assertThatThrownBy(() -> guard.authenticate(MISSING_EMAIL, "wrong-password", address))
                    .isInstanceOf(BadCredentialsException.class);
        }
        assertThatThrownBy(() -> guard.authenticate(MEMBER_EMAIL, "wrong-password", address))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("영구 차단");
        assertThat(attempts.findById(address).orElseThrow().getFailedAttempts()).isEqualTo(5);
        assertThat(blocks.existsById(address)).isFalse();

        assertThatThrownBy(() -> guard.authenticate(MEMBER_EMAIL, MATCHING_PASSWORD, "::ffff:" + address))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(members, times(1)).findMemberOrNull(MEMBER_EMAIL);
        assertThat(blocks.findById(address).orElseThrow().getBlockedByMemberId()).isNull();
        assertThat(attempts.findById(address).orElseThrow().getFailedAttempts()).isEqualTo(5);
    }

    @Test
    void aCorrectFifthAttemptResetsTheStoredCount() {
        String address = "203.0.113.51";
        for (int index = 0; index < 4; index++) {
            assertThatThrownBy(() -> guard.authenticate(MISSING_EMAIL, "wrong-password", address))
                    .isInstanceOf(BadCredentialsException.class);
        }

        assertThat(guard.authenticate(MEMBER_EMAIL, MATCHING_PASSWORD, address)).isSameAs(member);
        assertThat(attempts.findById(address).orElseThrow().getFailedAttempts()).isZero();
        assertThatThrownBy(() -> guard.authenticate(MISSING_EMAIL, "wrong-password", address))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(attempts.findById(address).orElseThrow().getFailedAttempts()).isEqualTo(1);
    }

    @Test
    void concurrentRequestsOnlyCheckFiveCredentialsForOneIp() throws Exception {
        String address = "203.0.113.52";
        var workers = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        List<Future<Integer>> requests = new ArrayList<>();
        try {
            for (int index = 0; index < 8; index++) {
                requests.add(workers.submit(() -> {
                    start.await();
                    try {
                        guard.authenticate(MISSING_EMAIL, "wrong-password", address);
                        return 200;
                    } catch (BadCredentialsException failure) {
                        return 401;
                    } catch (ResponseStatusException rejection) {
                        return rejection.getStatusCode().value();
                    }
                }));
            }
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (var request : requests) statuses.add(request.get(10, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(401, 401, 401, 401, 401, 403, 403, 403);
            verify(members, times(5)).findMemberOrNull(MISSING_EMAIL);
            assertThat(blocks.existsById(address)).isTrue();
        } finally {
            workers.shutdownNow();
        }
    }

    @TestConfiguration
    static class PasswordConfig {
        @Bean
        BCryptPasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }
}
