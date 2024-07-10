package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.board.BoardRequestDto;
import spring.study.entity.Board;
import spring.study.entity.Comment;
import spring.study.entity.Member;
import spring.study.service.BoardService;
import spring.study.service.CommentService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/board/")
public class BoardApiController {
    private final BoardService boardService;
    private final CommentService commentService;

    @PostMapping("/write/action")
    public Board boardWriteAction(@RequestBody BoardRequestDto boardRequestDto, HttpSession session) {
        Member member = (Member) session.getAttribute("member");
        Board result = null;

        if (boardRequestDto.getContent() != null){
            Board board = boardRequestDto.toEntity();

            board.addMember(member);

            result = boardService.save(board);
            session.setAttribute("member", member);

            if (result == null) {
                return null;
            }
        }
        else {
            return null;
        }

        return result;
    }

    @PatchMapping("/view/action")
    public int boardViewAction(@RequestBody BoardRequestDto boardRequestDto){
        return boardService.updateBoard(boardRequestDto.getId(), boardRequestDto.getTitle(), boardRequestDto.getContent());
    }

    @DeleteMapping("/view/delete")
    public void boardViewDeleteAction(@RequestParam() Long id, HttpSession session){
        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        member.removeComment(board.getComment());

        for (Board b : member.getBoard()) {
            if (b.getId().equals(board.getId())) {
                member.removeBoard(b);
                break;
            }
        }

        session.setAttribute("member", member);

        commentService.deleteComment(board);
        boardService.deleteById(id);
    }

    @DeleteMapping("/delete")
    public void boardDeleteAction(@RequestParam() Long[] deleteId) {
        boardService.deleteAll(deleteId);
    }
}
