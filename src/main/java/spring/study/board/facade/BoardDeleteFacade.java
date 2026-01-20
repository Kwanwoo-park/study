package spring.study.board.facade;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.favorite.service.FavoriteService;
import spring.study.member.entity.Member;
import spring.study.reply.service.ReplyService;

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
    public void deleteBoard(Long boardId, Member member) {
        Board board = boardService.findById(boardId);

        if (!board.getMember().getId().equals(member.getId()))
            throw new AccessDeniedException("본인 글만 삭제할 수 있습니다");

        favoriteService.deleteByBoard(board);
        replyService.deleteReplay(board.getComment());
        commentService.deleteComment(board);
        imageS3Service.deleteImage(board.getImg());
        boardImgService.deleteBoard(board);
        boardService.deleteById(boardId);
    }
}
