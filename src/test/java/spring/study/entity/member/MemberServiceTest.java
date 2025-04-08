//package spring.study.entity.member;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//import spring.study.dto.member.MemberRequestDto;
//import spring.study.dto.member.MemberResponseDto;
//import spring.study.entity.board.Board;
//import spring.study.service.member.MemberService;
//import spring.study.service.member.UserService;
//
//import java.util.HashMap;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.notNullValue;
//
//@SpringBootTest
//public class MemberServiceTest {
//    @Autowired
//    MemberService memberService;
//    @Autowired
//    UserService userService;
//
//
//    @Transactional
//    @Test
//    void save() {
//        //given
//        MemberRequestDto memberRequestDto = MemberRequestDto.builder()
//                .email("test2@test.com")
//                .password("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.img")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .build();
//
//        //when
//        MemberResponseDto responseDto = userService.createUser(memberRequestDto);
//
//        //then
//        assertThat(responseDto, is(notNullValue()));
//        assertThat(responseDto.getEmail()).isEqualTo(memberRequestDto.getEmail());
//    }
//
//
//    @Transactional
//    @Test
//    void update() {
//        //given
//        Member member = Member.builder()
//                .email("test@test.com")
//                .pwd("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member save = memberService.save(member);
//
//        //when
//        userService.updatePwd(save.getId(), "test2");
//        memberService.updateProfile(save.getId(), "2.jpg");
//        memberService.updateLastLoginTime(save.getId());
//
//        //then
//        Member result = memberService.findMember(save.getEmail());
//
//        assertThat(result.getPwd()).isEqualTo("test2");
//        assertThat(result.getProfile()).isEqualTo("2.jpg");
//    }
//
//    @Test
//    void findMember() {
//        // given
//        Member member = memberService.findMember("test@test.com");
//
//        String phone = "010-1234-1234";
//        String birth = "1900-01-01";
//
//        // when
//        Member result = memberService.findMember(phone, birth);
//
//        // then
//        assertThat(member.getName()).isEqualTo(result.getName());
//        assertThat(member.getEmail()).isEqualTo(result.getEmail());
//    }
//
//    @Test
//    void updatePhoneAndBirth() {
//        // given
//        Member member = memberService.findMember("test@test.com");
//
//        String date = "1900-01-01";
//
//        // when
//        memberService.updatePhoneAndBirth(member.getId(), "010-1234-1234", date);
//
//        // then
//        member = memberService.findMember("test@test.com");
//
//        assertThat(member.getBirth()).isEqualTo(date);
//        assertThat(member.getPhone()).isEqualTo("010-1234-1234");
//    }
//
//    @Transactional
//    @Test
//    void findAll() {
//        // given
//        Member member1 = Member.builder()
//                .email("test@test.com")
//                .pwd("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member member2 = Member.builder()
//                .email("test@test.com")
//                .pwd("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member save1 = memberService.save(member1);
//        Member save2 = memberService.save(member2);
//
//        // when
//        HashMap<String, Object> map = memberService.findAll(0, 5);
//
//        // then
//        for (String key : map.keySet()) {
//            System.out.println(map.get(key));
//        }
//    }
//
//    @Test
//    void find() {
//        Member member = (Member) memberService.loadUserByUsername("akakslslzz@naver.com");
//
//        for (Board b : member.getBoard())
//            System.out.println("Content = " +  b.getContent());
//    }
//}
