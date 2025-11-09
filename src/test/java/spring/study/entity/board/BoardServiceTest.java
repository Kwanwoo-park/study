//package spring.study.entity.board;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//import spring.study.board.dto.BoardResponseDto;
//import spring.study.board.entity.Board;
//import spring.study.board.service.BoardService;
//import spring.study.member.entity.Member;
//import spring.study.member.entity.Role;
//import spring.study.member.service.MemberService;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest()
//public class BoardServiceTest {
//    @Autowired
//    BoardService boardService;
//    @Autowired
//    MemberService memberService;
//
//    @Transactional
//    @Test
//    void update() {
//        // given
//        Member member = Member.builder()
//                .email("test@test.com")
//                .pwd("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member save = memberService.save(member);
//
//        Board board = Board.builder()
//                .title("test")
//                .content("test")
//                .build();
//
//        save.addBoard(board);
//
//        Board saveBoard = boardService.save(board);
//
//        // when
//        boardService.updateBoard(saveBoard.getId(), "test2");
//
//        // then
//        Board result = boardService.findById(board.getId());
//
//        assertThat(result.getContent()).isEqualTo("test2");
//        assertThat(result.getMember()).isEqualTo(save);
//    }
//
//    @Test
//    void findNextBoard() {
//        // given
//        LocalDateTime cursor = LocalDateTime.now();
//        int limit = 10;
//
//        // when
//        List<BoardResponseDto> list = boardService.getBoard(cursor, limit);
//
//        // then
//        if (!list.isEmpty()) {
//            for (BoardResponseDto b : list) {
//                System.out.println(b.getId() + " " + b.getContent() +" " +
//                        b.getMember() + " " + b.getImg());
//            }
//        } else {
//            System.out.println("List is empty");
//        }
//    }
//
//    @Test
//    void findAll() {
//        // given
//        Member member = Member.builder()
//                .email("test@test.com")
//                .pwd("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member save = memberService.save(member);
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
//        Board saveBoard1 = boardService.save(board1);
//        Board saveBoard2 = boardService.save(board2);
//
//        // when
//        HashMap<String, Object> map = boardService.findAll(0, 5);
//
//        // then
//        for (String key : map.keySet()) {
//            System.out.println(map.get(key).toString());
//        }
//    }
//
//    @Test
//    void deleteByMember() {
//        // given
//        Member member = memberService.findMember("test@test.com");
//        System.out.println(boardService.findAll().size());
//
//        // when
//        boardService.deleteByMember(member);
//
//        // then
//        System.out.println(boardService.findAll().size());
//    }
//}
