package spring.study.controller.board;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.board.BoardRequestDto;
import spring.study.entity.board.Board;
import spring.study.entity.board.BoardImg;
import spring.study.entity.forbidden.Status;
import spring.study.entity.member.Member;
import spring.study.service.aws.ImageS3Service;
import spring.study.service.board.BoardImgService;
import spring.study.service.board.BoardService;
import spring.study.service.comment.CommentService;
import spring.study.service.favorite.FavoriteService;
import spring.study.service.forbidden.ForbiddenService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/board")
public class BoardApiController {
    private final BoardService boardService;
    private final CommentService commentService;
    private final BoardImgService boardImgService;
    private final FavoriteService favoriteService;
    private final ImageS3Service imageS3Service;
    private final ForbiddenService forbiddenService;

    @PostMapping("/write")
    public ResponseEntity<Long> boardWriteAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");

        if (!boardRequestDto.getContent().isBlank() || !boardRequestDto.getContent().isEmpty()){
            if (forbiddenService.findWordList(Status.APPROVAL, boardRequestDto.getContent()))
                return ResponseEntity.ok(-1L);

            Board board = boardRequestDto.toEntity();

            board.addMember(member);

            Board result = boardService.save(board);
            session.setAttribute("member", member);

            if (result == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
            }

            return ResponseEntity.ok(result.getId());
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PatchMapping("/view")
    public ResponseEntity<Integer> boardViewAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        if (!boardRequestDto.getContent().isBlank() | !boardRequestDto.getContent().isEmpty()) {
            if (forbiddenService.findWordList(Status.APPROVAL, boardRequestDto.getContent()))
                return ResponseEntity.ok(-1);

            return ResponseEntity.ok(boardService.updateBoard(boardRequestDto.getId(), boardRequestDto.getContent()));
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @DeleteMapping("/view/delete")
    public ResponseEntity<Board> boardViewDeleteAction(@RequestParam() Long id, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        member.removeComments(board.getComment());
        member.removeBoard(board);

        session.setAttribute("member", member);

        favoriteService.deleteByBoard(board);
        commentService.deleteComment(board);

        List<BoardImg> images = boardService.findById(id).getImg();

        for (BoardImg img : images) {
            imageS3Service.deleteImage(img.getImgSrc());
        }

        boardImgService.deleteBoard(board);
        boardService.deleteById(id);

        return ResponseEntity.ok(board);
    }
}
