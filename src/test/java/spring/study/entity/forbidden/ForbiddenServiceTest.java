//package spring.study.entity.forbidden;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import spring.study.forbidden.entity.Status;
//import spring.study.forbidden.service.ForbiddenService;
//
//import static org.assertj.core.api.Assertions.*;
//
//@SpringBootTest
//public class ForbiddenServiceTest {
//    @Autowired
//    ForbiddenService forbiddenService;
//
//    @Test
//    void find() {
//        // given
//        String content = "씨발 뭐라는 거야";
//
//        // when
//        int risk = forbiddenService.findWordList(Status.APPROVAL, content);
//
//        // then
//        assertThat(risk).isEqualTo(3);
//    }
//}
