//package spring.study.entity.comment;
//
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import spring.study.entity.board.Board;
//import spring.study.entity.comment.Comment;
//import spring.study.entity.member.Member;
//import spring.study.service.board.BoardService;
//import spring.study.service.comment.CommentService;
//import spring.study.service.member.MemberService;
//
//import java.util.HashMap;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
//@SpringBootTest
//public class CommentServiceTest {
//    @Autowired
//    MemberService memberService;
//    @Autowired
//    BoardService boardService;
//    @Autowired
//    CommentService commentService;
//
//    @Transactional
//    @Test
//    void save() {
//        // given
//        Member member = memberService.findMember("akakslslzz@naver.com");
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
//        Comment save = commentService.save(comment);
//
//        // then
//        assertThat(save).isEqualTo(comment);
//    }
//
//    @Test
//    @Transactional
//    void findAll() {
//        // given
//        Member member = memberService.findMember("akakslslzz@naver.com");
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
//        board.addComment(comment1);
//
//        member.addComment(comment2);
//        board.addComment(comment2);
//
//        Comment save1 = commentService.save(comment1);
//        Comment save2 = commentService.save(comment2);
//
//        // when
//        HashMap<String, Object> map = commentService.findAll(0, 5);
//
//        // then
//        for (String key : map.keySet()) {
//            System.out.println(map.get(key).toString());
//        }
//    }
//
//    @Transactional
//    @Test
//    void update() {
//        // given
//        Long cid = 4L;
//        String comments = "test";
//        Comment before = commentService.findById(cid);
//
//        System.out.println(before.getComments());
//
//        // when
//        commentService.updateComments(cid, comments);
//
//        // then
//        Comment after = commentService.findById(cid);
//        assertThat(after.getComments()).isEqualTo(comments);
//    }
//
//    @Test
//    void delete() {
//        // given
//        Long cid = 5L;
//        Long bid = 21L;
//
//        Member member = memberService.findMember("test@test.com");
//        Board board = boardService.findById(bid);
//        Comment comment = commentService.findById(cid);
//
//        System.out.println(member.getComment().size());
//        System.out.println(board.getComment().size());
//
//        // when
//        member.removeComment(comment);
//        board.removeComment(comment);
//
//        commentService.deleteById(cid);
//
//        // then
//        System.out.println(member.getComment().size());
//        System.out.println(board.getComment().size());
//    }
//
//    @Test
//    void deleteByBoard() {
//        // given
//        Member member = memberService.findMember("akakslslzz@naver.com");
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
//        Comment comment3 = Comment.builder()
//                .comments("test2")
//                .build();
//
//        member.addComment(comment1);
//        board.addComment(comment1);
//
//        member.addComment(comment2);
//        board.addComment(comment2);
//
//        member.addComment(comment3);
//        board.addComment(comment3);
//
//        Comment save1 = commentService.save(comment1);
//        Comment save2 = commentService.save(comment2);
//        Comment save3 = commentService.save(comment3);
//
//        System.out.println(member.getComment().size());
//
//        List<Comment> comment = board.getComment();
//
//        // when
//        commentService.deleteComment(board);
//
//        member.removeComments(comment);
//
//        // then
//        System.out.println(member.getComment().size());
//    }
//
//    @Test
//    void deleteByMember() {
//        // given
//        Member member = memberService.findMember("test@test.com");
//        System.out.println(commentService.findAll().size());
//
//        // when
//        commentService.deleteByMember(member);
//
//        // then
//        System.out.println(commentService.findAll().size());
//    }
//}
