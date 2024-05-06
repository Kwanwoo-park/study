package spring.study.entity.member;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.service.MemberService;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class MemberRepositoryTest {
    @Autowired
    MemberService memberService;

    @Transactional
    @Test
    void save() {
        MemberRequestDto memberSaveDto = new MemberRequestDto();

        memberSaveDto.setEmail("test");
        memberSaveDto.setPassword("test");
        memberSaveDto.setName("test");
        memberSaveDto.setRole(Role.USER);
        memberSaveDto.setProfile("1.img");

        Long result = memberService.save(memberSaveDto);

        if (result > 0) {
            assertThat("#Success Save");
            find();
            findName();
        }
    }

    @Transactional
    @Test
    void update(){
        int result = memberService.updateMemberLastLogin("test", LocalDateTime.now());

        if (result > 0) {
            assertThat("Success update");
        }
    }

    void find() {
        Member member = new Member();
        member.setName("test");

        Member member2 = (Member) memberService.loadUserByUsername("test");

        assertThat(member.getName()).isEqualTo(member2.getName());
    }

    @Test
    void findName() {
        Map<String, Object> result = memberService.findName("test");

        if (result != null) {
            System.out.println("# Success findName() : " + result.toString());

            for (String s : result.keySet()) {
                System.out.println(result.get(s));
            }
        }
        else
            System.out.println("# Fail findAll() ~");
    }

    @Transactional
    @Test
    void findAll() {
        Map<String, Object> result = memberService.findAll(0, 5);

        if (result != null) {
            System.out.println("# Success findAll() : " + result.toString());

            for (String s : result.keySet()) {
                System.out.println(result.get(s));
            }
        }
        else
            System.out.println("# Fail findAll() ~");
    }

    @Transactional
    @Test
    void updatePassword() {
        Member member = new Member();
        member.setEmail("test");

        memberService.updateMemberPassword(member.getEmail(), "test");
    }
}
