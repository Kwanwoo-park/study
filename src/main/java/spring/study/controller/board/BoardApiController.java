package spring.study.controller.board;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.board.BoardRequestDto;
import spring.study.entity.board.Board;
import spring.study.entity.member.Member;
import spring.study.service.board.BoardImgService;
import spring.study.service.board.BoardService;
import spring.study.service.comment.CommentService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/board")
public class BoardApiController {
    private final BoardService boardService;
    private final CommentService commentService;
    private final BoardImgService boardImgService;

    @PostMapping("/write")
    public ResponseEntity<Board> boardWriteAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Board result = null;

        if (!boardRequestDto.getContent().isBlank() || !boardRequestDto.getContent().isEmpty()){
            Board board = boardRequestDto.toEntity();

            board.addMember(member);

            result = boardService.save(board);
            session.setAttribute("member", member);

            if (result == null) {
                return ResponseEntity.ok(result);
            }
        }
        else {
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/view")
    public ResponseEntity<Integer> boardViewAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        return ResponseEntity.ok(boardService.updateBoard(boardRequestDto.getId(), boardRequestDto.getContent()));
    }

    @DeleteMapping("/view/delete")
    public ResponseEntity<Board> boardViewDeleteAction(@RequestParam() Long id, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        member.removeComments(board.getComment());

        member.removeBoard(board);

        session.setAttribute("member", member);

        commentService.deleteComment(board);
        boardImgService.deleteBoard(board);
        boardService.deleteById(id);

        return ResponseEntity.ok(board);
    }
}
