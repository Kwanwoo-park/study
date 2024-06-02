package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.board.BoardRequestDto;
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
    public Long boardWriteAction(@RequestBody BoardRequestDto boardRequestDto, HttpSession session) {
        Member member = (Member) session.getAttribute("member");
        Long result;

        if (boardRequestDto.getContent() != null){
            boardRequestDto.setMember(member);
            result = boardService.save(boardRequestDto);

            if (result < 0) {
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
        return boardService.updateBoard(boardRequestDto);
    }

    @DeleteMapping("/view/delete")
    public void boardViewDeleteAction(@RequestParam() Long id){
        boardService.deleteById(id);
        commentService.deleteComment(id);
    }

    @DeleteMapping("/delete")
    public void boardDeleteAction(@RequestParam() Long[] deleteId) {
        boardService.deleteAll(deleteId);
    }
}
