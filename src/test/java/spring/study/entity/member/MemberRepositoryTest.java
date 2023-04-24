package spring.study.entity.member;

import jakarta.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.member.MemberRequestDto;
import spring.study.service.MemberService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class MemberRepositoryTest {
    @Autowired
    MemberService memberService;

    @Transactional
    @Test
    void save() {
        MemberRequestDto memberSaveDto = new MemberRequestDto();

        memberSaveDto.setEmail("akakslsl@naver.com");
        memberSaveDto.setPassword("zzqqwoo1310!");
        memberSaveDto.setName("박관우");

        Long result = memberService.save(memberSaveDto);

        if (result > 0) {
            assertThat("#Success Save");
        }
    }

    @Transactional
    @Test
    void update(){
        int result = memberService.updateMemberLastLogin("akakslslzz@naver.com", LocalDateTime.now());

        if (result > 0) {
            assertThat("Success update");
        }
    }

    @Transactional
    @Test
    void find() {
        Member member = new Member();
        member.setName("박관우");

        Member member2 = memberService.loadUserByUsername("akakslslzz@naver.com");

        assertThat(member.getName()).isEqualTo(member2.getName());
    }

    @Transactional
    @Test
    void updatePassword() {
        Member member = new Member();
        member.setEmail("akakslslzz@naver.com");

        memberService.updateMemberPassword(member.getEmail(), "zzqqwoo1310!");
    }
}
