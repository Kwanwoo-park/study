package spring.study.entity.board;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.board.BoardResponseDto;
import spring.study.entity.Board;
import spring.study.service.BoardService;

import java.util.List;
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
        boardSaveDto.setRegisterName("test");
        boardSaveDto.setRegisterEmail("test");

        Long result = boardService.save(boardSaveDto);

        if (result > 0) {
            System.out.println("# Success save() ~");
            findAll();
            findById(result);
            findByName(boardSaveDto.getRegisterName());
            findByEmail(boardSaveDto.getRegisterEmail());
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

    void findByName(String name) {
        List<Board> result = boardService.findName(name);

        if (result.size() > 0) {
            System.out.println("# Success findByName() ~");
            for (Board b : result) {
                System.out.println(b.getTitle() + " " + b.getRegisterName() + " " + b.getContent());
            }
        }
        else {
            System.out.println("# Fail findByName() ~");
        }
    }

    void findByEmail(String email) {
        List<Board> result = boardService.findEmail(email);

        if (result.size() > 0) {
            System.out.println("# Success findByEmail() ~");
            for (Board b : result) {
                System.out.println(b.getTitle() + " " + b.getRegisterName() + " " + b.getContent());
            }
        }
        else {
            System.out.println("# Fail findByEmail() ~");
        }
    }

    void updateBoard(Long id) {
        BoardRequestDto boardRequestDto = new BoardRequestDto();

        boardRequestDto.setId(id);
        boardRequestDto.setTitle("업데이트 제목");
        boardRequestDto.setContent("업데이트 내용");
        boardRequestDto.setRegisterName("작성자");

        int result = boardService.updateBoard(boardRequestDto);

        if (result > 0) {
            System.out.println("# Success updateBoard() ~");
        }
        else {
            System.out.println("# Fail updateBoard() ~");
        }
    }
}
