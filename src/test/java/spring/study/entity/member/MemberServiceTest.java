package spring.study.entity.member;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.service.MemberService;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MemberServiceTest {
    @Autowired
    MemberService memberService;

    @Transactional
    @Test
    void update() {
        //given
        Member member = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member save = memberService.save(member);

        //when
        memberService.updatePwd(save.getId(), "test2");
        memberService.updateProfile(save.getId(), "2.jpg");
        memberService.updateLastLoginTime(save.getId());

        //then
        Member result = memberService.findMember(save.getEmail());

        assertThat(result.getPwd()).isEqualTo("test2");
        assertThat(result.getProfile()).isEqualTo("2.jpg");
    }

    @Transactional
    @Test
    void findAll() {
        // given
        Member member1 = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member member2 = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member save1 = memberService.save(member1);
        Member save2 = memberService.save(member2);

        // when
        HashMap<String, Object> map = memberService.findAll(0, 5);

        // then
        for (String key : map.keySet()) {
            System.out.println(map.get(key));
        }
    }


}
