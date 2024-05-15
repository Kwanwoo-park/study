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
    void test() {
        List<Object[]> result = testService.getCommentWithMember("박관우");

        for(Object[] arr: result) {
            System.out.println(Arrays.toString(arr));
        }
    }
}
