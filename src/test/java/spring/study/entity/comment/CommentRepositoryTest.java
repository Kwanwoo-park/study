//package spring.study.entity.comment;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.data.domain.Sort;
//import spring.study.entity.board.Board;
//import spring.study.entity.comment.Comment;
//import spring.study.entity.member.Member;
//import spring.study.repository.comment.CommentRepository;
//import spring.study.repository.member.MemberRepository;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
//        connection = EmbeddedDatabaseConnection.H2)
//public class CommentRepositoryTest {
//    @Autowired
//    CommentRepository commentRepository;
//    @Autowired
//    MemberRepository memberRepository;
//
//    @Test
//    void save() {
//        // given
//        Member member = memberRepository.findByEmail("akakslslzz@naver.com");
//
//        Board board = member.getBoard().get(0);
//
//        Comment comment = Comment.builder()
//                .comments("tests")
//                .build();
//
//        member.addComment(comment);
//        board.addComment(comment);
//
//        // when
//        Comment save = commentRepository.save(comment);
//
//        // then
//        assertThat(save.getComments()).isEqualTo(comment.getComments());
//        assertThat(save.getBoard()).isEqualTo(board);
//        assertThat(save.getMember()).isEqualTo(member);
//    }
//
//    //@Transactional
//    @Test
//    void findAll() {
//        // given
//        Member member = memberRepository.findByEmail("akakslslzz@naver.com");
//
//        Board board = member.getBoard().get(0);
//
//        Comment comment1 = Comment.builder()
//                .comments("test1")
//                .build();
//
//        Comment comment2 = Comment.builder()
//                .comments("test2")
//                .build();
//
//        member.addComment(comment1);
//        member.addComment(comment2);
//
//        board.addComment(comment1);
//        board.addComment(comment2);
//
//        Comment saveComment1 = commentRepository.save(comment1);
//        Comment saveComment2 = commentRepository.save(comment2);
//
//        // when
//        List<Comment> result = commentRepository.findAll(Sort.by("id").ascending());
//
//        // then
//        assertThat(result.get(0).getComments()).isEqualTo(saveComment1.getComments());
//        assertThat(result.get(0).getMember()).isEqualTo(member);
//        assertThat(result.get(0).getBoard()).isEqualTo(board);
//
//        assertThat(result.get(1).getComments()).isEqualTo(saveComment2.getComments());
//        assertThat(result.get(1).getMember()).isEqualTo(member);
//        assertThat(result.get(1).getBoard()).isEqualTo(board);
//    }
//
//    @Test
//    void find() {
//        // given
//        Member member = memberRepository.findByEmail("akakslslzz@naver.com");
//
//        Board board= member.getBoard().get(0);
//
//        Comment comment = Comment.builder()
//                .comments("test")
//                .build();
//
//        member.addComment(comment);
//        board.addComment(comment);
//
//        Comment save = commentRepository.save(comment);
//
//        // when
//        List<Comment> result = commentRepository.findByBoard(board);
//
//        // then
//        assertThat(result.get(0).getBoard()).isEqualTo(save.getBoard());
//        assertThat(result.get(0).getMember()).isEqualTo(save.getMember());
//    }
//
//
//    @Test
//    void delete() {
//        // given
//        Member member = memberRepository.findByEmail("akakslslzz@naver.com");
//        Board board = member.getBoard().get(0);
//
//        Comment comment = board.getComment().get(0);
//
//        // when
//        commentRepository.deleteByBoard(board);
//
//        member.getComment().remove(comment);
//        board.getComment().remove(comment);
//
//        // then
//        System.out.println(member.getComment().size());
//        System.out.println(board.getComment().size());
//    }
//
//    @Test
//    void deleteByMember() {
//        // given
//        Member member = memberRepository.findByEmail("test@test.com");
//        System.out.println(commentRepository.findAll().size());
//
//        // when
//        commentRepository.deleteByMember(member);
//
//        // then
//        System.out.println(commentRepository.findAll().size());
//    }
//}
