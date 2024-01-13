package spring.study.entity.comment;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.service.CommentService;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class CommentRepositoryTest {
    @Autowired
    CommentService commentService;

    @Transactional
    @Test
    void save() {
        CommentRequestDto commentSaveDto = new CommentRequestDto();

        commentSaveDto.setComment("test");
        commentSaveDto.setMid(1L);
        commentSaveDto.setMname("박관우");
        commentSaveDto.setBid(19L);

        Long result = commentService.save(commentSaveDto);

        if (result > 0) {
            System.out.println("# Success save() ~");
            findAll();
            findComment(commentSaveDto.getBid());
        }
        else {
            System.out.println("# Fail save()");
        }
    }

    void findAll() {
        Map<String, Object> result = commentService.findAll();

        if (result != null) {
            System.out.println("# Success findAll() : " + result.toString());

            for (String s : result.keySet()) {
                System.out.println(result.get(s));
            }
        }
        else {
            System.out.println("# Fail findAll() ~");
        }
    }

    void findComment(Long bid) {
        String mname = "박관우";
        Comment comment = new Comment();
        comment.setMname(mname);

        Comment comment1 = commentService.findComment(bid);

        assertThat(comment.getMname()).isEqualTo(comment1.getMname());
    }
}
