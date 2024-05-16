package spring.study;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.service.TestService;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class StudyApplicationTests {
    @Autowired
    private TestService testService;

    @Test
    void getComment() {
        List<Object[]> result = testService.getCommentWithMember(1234567890L);

        for(Object[] arr : result) {
            System.out.println(Arrays.toString(arr));
        }
    }

    @Test
    void getMessage() {
        List<Object[]> result = testService.getMessageWithMember("roomId");

        for(Object[] arr : result) {
            System.out.println(Arrays.toString(arr));
        }
    }
}
