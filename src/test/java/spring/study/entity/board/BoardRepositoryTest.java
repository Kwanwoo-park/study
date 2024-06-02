package spring.study.entity.board;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.repository.BoardRepository;
import spring.study.repository.MemberRepository;

import static org.assertj.core.api.Assertions.*;
@SpringBootTest
public class BoardRepositoryTest {
    @Autowired
    BoardRepository boardRepository;
    @Autowired
    MemberRepository memberRepository;

    @Transactional
    @Test
    void save() {
        Member member = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member save = memberRepository.save(member);
    }
}
