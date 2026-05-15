//package spring.study.entity.notificaiton;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import spring.study.member.entity.Member;
//import spring.study.member.service.MemberService;
//import spring.study.notification.entity.Notification;
//import spring.study.notification.service.NotificationService;
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
//
//    @Test
//    void count() {
//        // given
//        Member member = memberService.findMember("akakslslzz@naver.com");
//
//        // when
//        Long count = notificationService.countUnReadNotification(member);
//
//        // then
//        System.out.println(count);
//    }
//}
