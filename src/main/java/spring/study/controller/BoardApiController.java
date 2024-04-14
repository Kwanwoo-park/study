package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.alert.AlertMessage;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.entity.Member;
import spring.study.service.BoardService;
import spring.study.service.CommentService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/board/")
public class BoardApiController {
    private final BoardService boardService;
    private Member member;
    private final CommentService commentService;
    @PostMapping("/write/action")
    public Long boardWriteAction(@RequestBody BoardRequestDto boardRequestDto,
                                   HttpServletRequest request,
                                   Model model) throws Exception {
        HttpSession session = request.getSession();
        member = (Member) session.getAttribute("member");
        Long result = 0L;

        try {
            if (boardRequestDto.getContent() != null){
                boardRequestDto.setRegisterId(member.getName());
                boardRequestDto.setRegisterEmail(member.getEmail());
                result = boardService.save(boardRequestDto);

                if (result < 0) {
                    return null;
                }
            }
            else {
                return null;
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return result;
    }

    @PatchMapping("/view/action")
    public int boardViewAction(@RequestParam() Long id, @RequestBody BoardRequestDto boardRequestDto) throws Exception {
        int result;

        try {
            result = boardService.updateBoard(boardRequestDto);
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        return result;
    }

    @PostMapping("/comment/{bid}/action")
    public Long commentAction(@PathVariable Long bid,
                              @RequestBody CommentRequestDto commentRequestDto,
                              HttpServletRequest request) throws Exception {
        long result = 0;

        HttpSession session = request.getSession();
        member = (Member) session.getAttribute("member");

        try {
            if (commentRequestDto.getComment() != null){
                commentRequestDto.setBid(bid);
                commentRequestDto.setMid(member.getId());
                commentRequestDto.setMname(member.getName());
                commentRequestDto.setEmail(member.getEmail());

                result = commentService.save(commentRequestDto);
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return result;
    }

    @DeleteMapping("/view/delete")
    public void boardViewDeleteAction(@RequestParam() Long id, Model model) throws Exception {
        try {
            boardService.deleteById(id);
            commentService.deleteComment(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public void boardDeleteAction(@RequestParam() Long[] deleteId) throws Exception {
        try {
            boardService.deleteAll(deleteId);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }
}
