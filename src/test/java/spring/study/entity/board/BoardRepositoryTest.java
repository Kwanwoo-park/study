package spring.study.entity.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.repository.BoardRepository;

import static org.assertj.core.api.Assertions.*;
@SpringBootTest
public class BoardRepositoryTest {
    @Autowired
    BoardRepository boardRepository;


}
