package spring.study.entity.board.member;

import jakarta.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.member.MemberRequestDto;
import spring.study.service.MemberService;

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
}
