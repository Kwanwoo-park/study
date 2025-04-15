//package spring.study.entity.notificaiton;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import spring.study.entity.member.Member;
//import spring.study.entity.notification.Notification;
//import spring.study.repository.member.MemberRepository;
//import spring.study.repository.notification.NotificationRepository;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
//    connection = EmbeddedDatabaseConnection.H2)
//public class NotificationRepositoryTest {
//    @Autowired
//    NotificationRepository notificationRepository;
//    @Autowired
//    MemberRepository memberRepository;
//
//    @Test
//    void find() {
//        //given
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//
//        //when
//        List<Notification> list = notificationRepository.findByMember(member);
//
//        //then
//        for (Notification noti : list) {
//            System.out.println(noti.getMember().getEmail() + " " + noti.getMessage());
//        }
//    }
//}
