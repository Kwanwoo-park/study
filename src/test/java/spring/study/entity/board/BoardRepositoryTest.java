package spring.study.entity.board;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.board.BoardResponseDto;
import spring.study.service.BoardService;
import java.util.Map;

@SpringBootTest
class BoardRepositoryTest {

    @Autowired
    private BoardService boardService;

    @Transactional
    @Test
    void save() {
        BoardRequestDto boardSaveDto = new BoardRequestDto();

        boardSaveDto.setTitle("제목입니다.");
        boardSaveDto.setContent("내용입니다.");
        boardSaveDto.setRegisterId("박관우");
        boardSaveDto.setRegisterEmail("akakslslzz@naver.com");

        Long result = boardService.save(boardSaveDto);

        if (result > 0) {
            System.out.println("# Success save() ~");
            findAll();
            findById(result);
            updateBoard(boardSaveDto.getId());
        }
        else {
            System.out.println("# Fail save()");
        }
    }

    @Test
    void findAll() {
        Map<String, Object> result = boardService.findAll(0, 5);

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

    void findById(Long id) {
        BoardResponseDto info = boardService.findById(id);

        if (info != null) {
            System.out.println("# Success findById() : " + info.toString());
        }
        else {
            System.out.println("# Fail findById() ~");
        }
    }

    void updateBoard(Long id) {
        BoardRequestDto boardRequestDto = new BoardRequestDto();

        boardRequestDto.setId(id);
        boardRequestDto.setTitle("업데이트 제목");
        boardRequestDto.setContent("업데이트 내용");
        boardRequestDto.setRegisterId("박관우");

        int result = boardService.updateBoard(boardRequestDto);

        if (result > 0) {
            System.out.println("# Success updateBoard() ~");
        }
        else {
            System.out.println("# Fail updateBoard() ~");
        }
    }
}
