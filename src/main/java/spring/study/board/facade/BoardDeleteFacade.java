package spring.study.board.facade;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.favorite.service.FavoriteService;
import spring.study.member.entity.Member;
import spring.study.reply.service.ReplyService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BoardDeleteFacade {
    private final BoardService boardService;
    private final ReplyService replyService;
    private final CommentService commentService;
    private final BoardImgService boardImgService;
    private final FavoriteService favoriteService;
    private final ImageS3Service imageS3Service;

    @Transactional
    public ResponseEntity<?> deleteBoard(Long boardId, Member member) {
        Board board = boardService.findById(boardId);

        if (!board.getMember().getId().equals(member.getId()))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "result", -1,
                "message", "본인 게시글만 지울 수 있습니다"
            ));

        favoriteService.deleteByBoard(board);
        replyService.deleteReplay(board.getComment());
        commentService.deleteComment(board);
        imageS3Service.deleteImage(board.getImg());
        boardImgService.deleteBoard(board);
        boardService.deleteById(boardId);

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }
}
