package spring.study.member.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY,
        connection = EmbeddedDatabaseConnection.H2
)
class MemberAudioCallPreferencePersistenceTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void preferenceShouldDefaultToEnabledAndPersistDisabledState() {
        Member member = memberRepository.saveAndFlush(Member.builder()
                .email("member@example.com")
                .pwd("encoded-password")
                .name("member")
                .role(Role.USER)
                .phone("01012345678")
                .birth("20000101")
                .profile("profile.png")
                .build());
        assertThat(member.isAudioCallEnabled()).isTrue();

        member.changeAudioCallEnabled(false);
        memberRepository.saveAndFlush(member);
        entityManager.clear();

        Member restored = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(restored.isAudioCallEnabled()).isFalse();
    }
}
