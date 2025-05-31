//package spring.study.entity.notificaiton;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import spring.study.entity.member.Member;
//import spring.study.notification.entity.Notification;
//import spring.study.service.member.MemberService;
//import spring.study.service.notification.NotificationService;
//
//import java.util.List;
//
//@SpringBootTest
//public class NotificationServiceTest {
//    @Autowired
//    NotificationService notificationService;
//    @Autowired
//    MemberService memberService;
//
//    @Test
//    void find() {
//        //given
//        Member member = memberService.findMember("akakslslzz@naver.com");
//
//        //when
//        List<Notification> list = notificationService.findByMember(member);
//
//        //then
//        for (Notification noti : list) {
//            System.out.println(noti.getMember().getEmail() + " " + noti.getMessage());
//        }
//    }
//}
