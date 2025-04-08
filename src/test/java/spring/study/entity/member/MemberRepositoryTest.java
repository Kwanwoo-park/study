//package spring.study.entity.member;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.data.domain.Sort;
//import spring.study.repository.member.MemberRepository;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
//        connection = EmbeddedDatabaseConnection.H2)
//public class MemberRepositoryTest {
//    @Autowired
//    MemberRepository memberRepository;
//
//
//    @Test
//    void save() {
//        //given
//        Member member = Member.builder()
//                .email("test2@test.com")
//                .pwd("test")
//                .name("test")
//                .phone("010-1234-1234")
//                .birth("1900-01-02")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        //when
//        Member save = memberRepository.save(member);
//
//        //then
//        assertThat(save.getName()).isEqualTo(member.getName());
//        assertThat(save.getEmail()).isEqualTo(member.getEmail());
//    }
//
//    @Test
//    void find() {
//        //given
//        Member member = Member.builder()
//                .email("test2@test.com")
//                .pwd("test")
//                .name("test")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member save = memberRepository.save(member);
//
//        //when
//        Member result = memberRepository.findByEmail("test@test.com").orElseThrow();
//
//        //then
//        assertThat(result.getName()).isEqualTo(save.getName());
//    }
//
//    @Test
//    void findAll() {
//        //given
//        Member member1 = Member.builder()
//                .email("test2@test.com")
//                .pwd("test")
//                .name("test")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member member2 = Member.builder()
//                .email("test3@test.com")
//                .pwd("test")
//                .name("test2")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("2.jpg")
//                .build();
//
//        memberRepository.save(member1);
//        memberRepository.save(member2);
//
//        //when
//        List<Member> memberList = memberRepository.findAll(Sort.by("id").ascending());
//
//        //Then
//        for (Member m : memberList)
//            System.out.println(m.getName() + " " + m.getEmail());
//    }
//
//    @Test
//    void findName() {
//        //given
//        Member member1 = Member.builder()
//                .email("test2@test.com")
//                .pwd("test")
//                .name("test")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member member2 = Member.builder()
//                .email("test3@test.com")
//                .pwd("test")
//                .name("test")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("2.jpg")
//                .build();
//
//        Member save1 = memberRepository.save(member1);
//        Member save2 = memberRepository.save(member2);
//
//        //when
//        List<Member> result = memberRepository.findByName("test");
//
//        //then
//        assertThat(save1.getEmail()).isEqualTo(result.get(1).getEmail());
//        assertThat(save2.getEmail()).isEqualTo(result.get(2).getEmail());
//    }
//
//    @Test
//    void findByPhoneAndBirth() {
//        // given
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//
//        String phone = "010-1234-1234";
//        String birth = "1900-01-01";
//
//        // when
//        Member result = memberRepository.findByPhoneAndBirth(phone, birth);
//
//        // then
//        assertThat(member.getEmail()).isEqualTo(result.getEmail());
//        assertThat(member.getName()).isEqualTo(result.getName());
//    }
//
//    @Test
//    void test() {
//        System.out.println(memberRepository.findByEmail("test2@test.com"));
//    }
//}
