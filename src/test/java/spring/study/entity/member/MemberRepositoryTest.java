package spring.study.entity.member;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.repository.MemberRepository;
import spring.study.service.MemberService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class MemberRepositoryTest {
    @Autowired
    MemberRepository memberRepository;

    @Transactional
    @Test
    void save() {
        //given
        Member member = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        //when
        Member save = memberRepository.save(member);

        //then
        assertThat(save.getName()).isEqualTo(member.getName());
        assertThat(save.getEmail()).isEqualTo(member.getEmail());
    }

    @Transactional
    @Test
    void find() {
        //given
        Member member = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member save = memberRepository.save(member);

        //when
        Member result = memberRepository.findByEmail("test@test.com");

        //then
        assertThat(result.getName()).isEqualTo(save.getName());
    }

    @Transactional
    @Test
    void findAll() {
        //given
        Member member1 = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member member2 = Member.builder()
                .email("test2@test.com")
                .pwd("test")
                .name("test2")
                .role(Role.USER)
                .profile("2.jpg")
                .build();

        Member save1 = memberRepository.save(member1);
        Member save2 = memberRepository.save(member2);

        //when
        List<Member> memberList = memberRepository.findAll(Sort.by("id").ascending());

        //Then
        assertThat(save1.getName()).isEqualTo(memberList.get(0).getName());
        assertThat(save2.getName()).isEqualTo(memberList.get(1).getName());

        assertThat(save1.getEmail()).isEqualTo(memberList.get(0).getEmail());
        assertThat(save2.getEmail()).isEqualTo(memberList.get(1).getEmail());
    }

    @Transactional
    @Test
    void findName() {
        //given
        Member member1 = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member member2 = Member.builder()
                .email("test2@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("2.jpg")
                .build();

        Member save1 = memberRepository.save(member1);
        Member save2 = memberRepository.save(member2);

        //when
        List<Member> result = memberRepository.findByName("test");

        //then
        assertThat(save1.getEmail()).isEqualTo(result.get(0).getEmail());
        assertThat(save2.getEmail()).isEqualTo(result.get(1).getEmail());
    }
}
