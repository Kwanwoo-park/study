package spring.study.board.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.dto.BoardResponseDto;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.common.service.ModerationService;
import spring.study.favorite.service.FavoriteService;
import spring.study.member.entity.Member;
import spring.study.reply.service.ReplyService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardFacade {
    private final BoardService boardService;
    private final ReplyService replyService;
    private final CommentService commentService;
    private final BoardImgService boardImgService;
    private final FavoriteService favoriteService;
    private final ImageS3Service imageS3Service;
    private final ModerationService moderationService;

    public ResponseEntity<?> load(int cursor, int limit, Member member) {
        List<BoardResponseDto> list = boardService.getBoard(cursor, limit, member);
        int nextCursor = list.isEmpty() ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "boards", list,
                "nextCursor", nextCursor,
                "like", member.checkFavorite(list),
                "result", 10L
        ));
    }

    public ResponseEntity<?> detail(Long id, Member member) {
        if (!boardService.existBoard(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "result", -1,
                    "message", "존재하지 않는 게시글"
            ));
        }

        Board board = boardService.findById(id);
        long[] ids = boardService.getBoardIdList(id, board.getMember());

        return ResponseEntity.ok(Map.of(
                "result", 10,
                "board", new BoardResponseDto(board),
                "like", member.checkFavorite(board),
                "previous", ids[0],
                "next", ids[1]
        ));
    }

    public ResponseEntity<?> write(BoardRequestDto dto, Member member, HttpServletRequest request) {
        int risk = moderationService.validate(dto.getContent(), member, request);

        if (risk != 0) {
            if (risk == -99)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", risk,
                        "message", "게시글 내용이 없습니다"
                ));

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        Board board = dto.toEntity();
        board.addMember(member);

        Board saved = boardService.save(board);

        if (saved == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -10,
                    "message", "게시글이 저장되지 않았습니다"
            ));

        return ResponseEntity.ok(Map.of(
                "result", saved.getId()
        ));
    }

    public ResponseEntity<?> update(BoardRequestDto dto, Member member, HttpServletRequest request) {
        int risk = moderationService.validate(dto.getContent(), member, request);

        if (risk != 0) {
            if (risk == -99) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", risk,
                        "message", "게시글 내용이 없습니다"
                ));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", boardService.updateBoard(dto.getId(), dto.getContent())
        ));
    }

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
                "result", 1L,
                "email", member.getEmail()
        ));
    }
}
