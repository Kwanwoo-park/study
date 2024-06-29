package spring.study.entity.board;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.Board;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.repository.BoardRepository;
import spring.study.repository.MemberRepository;
import spring.study.service.BoardService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
@SpringBootTest
public class BoardRepositoryTest {
    @Autowired
    BoardRepository boardRepository;
    @Autowired
    BoardService boardService;
    @Autowired
    MemberRepository memberRepository;

    @Transactional
    @Test
    void save() {
        // given
        Member member = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member save = memberRepository.save(member);

        Board board = Board.builder()
                .title("test")
                .content("test")
                .build();

        save.addBoard(board);

        // when
        Board saveBoard = boardRepository.save(board);

        // then
        assertThat(saveBoard.getTitle()).isEqualTo(board.getTitle());
        assertThat(saveBoard.getContent()).isEqualTo(board.getContent());
        assertThat(saveBoard.getMember()).isEqualTo(save);
        assertThat(save.getBoard().get(0)).isEqualTo(saveBoard);
    }

    @Transactional
    @Test
    void findAll() {
        // given
        Member member = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member save = memberRepository.save(member);

        Board board1 = Board.builder()
                .title("test")
                .content("test")
                .build();

        Board board2 = Board.builder()
                .title("test")
                .content("test")
                .build();

        save.addBoard(board1);
        save.addBoard(board2);

        Board saveBoard1 = boardRepository.save(board1);
        Board saveBoard2 = boardRepository.save(board2);

        // when
        List<Board> result = boardRepository.findAll(Sort.by("id").ascending());

        // then
        assertThat(result.get(0).getTitle()).isEqualTo(saveBoard1.getTitle());
        assertThat(result.get(1).getTitle()).isEqualTo(saveBoard2.getTitle());

        assertThat(result.get(0).getContent()).isEqualTo(saveBoard1.getContent());
        assertThat(result.get(1).getContent()).isEqualTo(saveBoard2.getContent());

        assertThat(result.get(0).getMember()).isEqualTo(save);
        assertThat(result.get(1).getMember()).isEqualTo(save);
    }

    @Transactional
    @Test
    void find() {
        // given
        Member member = Member.builder()
                .email("test@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member save = memberRepository.save(member);

        Board board = Board.builder()
                .title("test")
                .content("test")
                .build();

        save.addBoard(board);

        Board saveBoard = boardRepository.save(board);

        // when
        Board result = boardRepository.findByTitle(saveBoard.getTitle());

        // then
        assertThat(result.getMember()).isEqualTo(saveBoard.getMember());
        assertThat(result).isEqualTo(saveBoard);
        assertThat(result.getMember()).isEqualTo(save);
    }
}
