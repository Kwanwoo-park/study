//package spring.study.entity.board;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.data.domain.Sort;
//import spring.study.entity.board.Board;
//import spring.study.entity.member.Member;
//import spring.study.entity.member.Role;
//import spring.study.repository.board.BoardRepository;
//import spring.study.repository.member.MemberRepository;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
//        connection = EmbeddedDatabaseConnection.H2)
//public class BoardRepositoryTest {
//    @Autowired
//    BoardRepository boardRepository;
//    @Autowired
//    MemberRepository memberRepository;
//
//    @Test
//    void save() {
//        // given
//        Member member = Member.builder()
//                .email("test2@test.com")
//                .pwd("test")
//                .name("test")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member save = memberRepository.save(member);
//
//        Board board = Board.builder()
//                .title("test")
//                .content("test")
//                .build();
//
//        save.addBoard(board);
//
//        // when
//        Board saveBoard = boardRepository.save(board);
//
//        // then
//        assertThat(saveBoard.getTitle()).isEqualTo(board.getTitle());
//        assertThat(saveBoard.getContent()).isEqualTo(board.getContent());
//        assertThat(saveBoard.getMember()).isEqualTo(save);
//        assertThat(save.getBoard().get(0)).isEqualTo(saveBoard);
//    }
//
//    @Test
//    void findAll() {
//        // given
//        Member member = Member.builder()
//                .email("test2@test.com")
//                .pwd("test")
//                .name("test")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member save = memberRepository.save(member);
//
//        Board board1 = Board.builder()
//                .title("test")
//                .content("test")
//                .build();
//
//        Board board2 = Board.builder()
//                .title("test")
//                .content("test")
//                .build();
//
//        save.addBoard(board1);
//        save.addBoard(board2);
//
//        Board saveBoard1 = boardRepository.save(board1);
//        Board saveBoard2 = boardRepository.save(board2);
//
//        // when
//        List<Board> result = boardRepository.findAll(Sort.by("id").ascending());
//
//        // then
//        for (Board b : result)
//            System.out.println(b.getTitle() + " " + b.getContent());
//    }
//
//    @Test
//    void find() {
//        // given
//        Member member = Member.builder()
//                .email("test2@test.com")
//                .pwd("test")
//                .name("test")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member save = memberRepository.save(member);
//
//        Board board = Board.builder()
//                .title("test")
//                .content("test")
//                .build();
//
//        save.addBoard(board);
//
//        Board saveBoard = boardRepository.save(board);
//
//        // when
//        List<Board> result = boardRepository.findByTitle(saveBoard.getTitle());
//
//        // then
//        for (Board b : result)
//            System.out.println(b.getTitle() + " " + b.getContent());
//    }
//
//    @Test
//    void deleteByMember() {
//        // given
//        Member member = memberRepository.findByEmail("test@test.com");
//        System.out.println(boardRepository.findAll().size());
//
//        // when
//
//        boardRepository.deleteByMember(member);
//
//        // then
//        System.out.println(boardRepository.findAll().size());
//    }
//}
