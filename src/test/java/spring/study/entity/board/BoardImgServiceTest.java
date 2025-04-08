//package spring.study.entity.board;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import spring.study.service.board.BoardImgService;
//import spring.study.service.board.BoardService;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
//@SpringBootTest
//public class BoardImgServiceTest {
//    @Autowired
//    BoardService boardService;
//    @Autowired
//    BoardImgService boardImgService;
//
//    @Test
//    void save() {
//        // given
//        Board board = boardService.findById(10L);
//
//        BoardImg boardImg = BoardImg.builder()
//                .imgSrc("1.jpg")
//                .board(board)
//                .build();
//
//        // when
//        board.addImg(boardImg);
//
//        BoardImg saveImg = boardImgService.save(boardImg);
//
//        // then
//        assertThat(boardImg.getImgSrc()).isEqualTo(saveImg.getImgSrc());
//        assertThat(boardImg.getBoard()).isEqualTo(saveImg.getBoard());
//    }
//
//    @Test
//    void find() {
//        // given
//        Board board = boardService.findById(10L);
//
//        BoardImg boardImg1 = BoardImg.builder()
//                .imgSrc("1.jpg")
//                .board(board)
//                .build();
//
//        BoardImg boardImg2 = BoardImg.builder()
//                .imgSrc("2.jpg")
//                .board(board)
//                .build();
//
//        board.addImg(boardImg1);
//        board.addImg(boardImg2);
//
//        BoardImg saveImg1 = boardImgService.save(boardImg1);
//        BoardImg saveImg2 = boardImgService.save(boardImg2);
//
//        // when
//        List<BoardImg> list = boardImgService.findBoard(board);
//
//        // then
//        for (BoardImg img : list) {
//            System.out.println(img.getId() + " " + img.getImgSrc());
//        }
//    }
//
//    @Test
//    void delete() {
//        // given
//        Board board = boardService.findById(10L);
//
//        System.out.println(boardImgService.findBoard(board).size());
//
//        // when
//        boardImgService.deleteBoard(board);
//
//        // then
//        System.out.println(boardImgService.findBoard(board).size());
//    }
//}
