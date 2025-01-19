//package spring.study.entity.board;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import spring.study.entity.board.Board;
//import spring.study.entity.board.BoardImg;
//import spring.study.repository.board.BoardImgRepository;
//import spring.study.repository.board.BoardRepository;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
//        connection = EmbeddedDatabaseConnection.H2)
//public class BoardImgRepositoryTest {
//    @Autowired
//    BoardRepository boardRepository;
//
//    @Autowired
//    BoardImgRepository boardImgRepository;
//
//    @Test
//    void save() {
//        // given
//        Board board = boardRepository.findById(10L).orElseThrow();
//
//        BoardImg boardImg = BoardImg.builder()
//                .board(board)
//                .imgSrc("KakaoTalk_Photo_2023-04-14-21-36-15.jpeg")
//                .build();
//
//        // when
//        board.addImg(boardImg);
//
//        BoardImg saveBoardImg = boardImgRepository.save(boardImg);
//
//        // then
//        assertThat(saveBoardImg.getBoard()).isEqualTo(board);
//        assertThat(saveBoardImg.getImgSrc()).isEqualTo(boardImg.getImgSrc());
//    }
//
//    @Test
//    void find() {
//        // given
//        Board board = boardRepository.findById(10L).orElseThrow();
//
//        BoardImg boardImg1 = BoardImg.builder()
//                .board(board)
//                .imgSrc("KakaoTalk_Photo_2023-04-14-21-36-15.jpeg")
//                .build();
//
//        BoardImg boardImg2 = BoardImg.builder()
//                .board(board)
//                .imgSrc("IMG_2371.jpeg")
//                .build();
//
//        board.addImg(boardImg1);
//        board.addImg(boardImg2);
//
//        BoardImg saveImg1 = boardImgRepository.save(boardImg1);
//        BoardImg saveImg2 = boardImgRepository.save(boardImg2);
//
//        // when
//        List<BoardImg> list = boardImgRepository.findByBoard(board);
//
//        // then
//        for (BoardImg boardImg : list)
//            System.out.println(boardImg.getId() + " " + boardImg.getImgSrc());
//    }
//
//    @Test
//    void delete() {
//        // given
//        Board board = boardRepository.findById(10L).orElseThrow();
//
//        BoardImg boardImg1 = BoardImg.builder()
//                .board(board)
//                .imgSrc("KakaoTalk_Photo_2023-04-14-21-36-15.jpeg")
//                .build();
//
//        BoardImg boardImg2 = BoardImg.builder()
//                .board(board)
//                .imgSrc("IMG_2371.jpeg")
//                .build();
//
//        board.addImg(boardImg1);
//        board.addImg(boardImg2);
//
//        boardImgRepository.save(boardImg1);
//        boardImgRepository.save(boardImg2);
//
//        System.out.println(boardImgRepository.findByBoard(board).size());
//
//        // when
//        boardImgRepository.deleteByBoard(board);
//
//        // then
//        System.out.println(boardImgRepository.findByBoard(board).size());
//    }
//}
