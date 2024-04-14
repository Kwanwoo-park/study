package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.alert.AlertMessage;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.entity.Member;
import spring.study.service.BoardService;
import spring.study.service.CommentService;

@RequiredArgsConstructor
@Controller
@RequestMapping("/board")
public class BoardViewController {
    private final BoardService boardService;
    private Member member;
    private final CommentService commentService;
    private HttpSession session;

    @GetMapping("/list")
    public String getBoardListPage(Model model,
                                   HttpServletRequest request,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size) throws Exception {

        session = request.getSession();
        member = (Member) session.getAttribute("member");

        if (session == null)
            return "redirect:/login";

        if (member == null) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            model.addAttribute("resultMap", boardService.findAll(page, size));
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/list";
    }

    @GetMapping("/write")
    public String getBoardWritePage(Model model){
        if (session == null) {
            member = null;
            return "redirect:/login?error=true&exception=Login Please";
        }

        model.addAttribute("name", member.getName());
        return "/board/write";
    }

    @GetMapping("/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto) throws Exception {
        try {
            if (member == null) {
                session.invalidate();
                return "redirect:/login?error=true&exception=Login Please";
            }

            if (boardRequestDto.getId() != null) {
                model.addAttribute("info", boardService.findById(boardRequestDto.getId()));
                model.addAttribute("email", member.getEmail());
                model.addAttribute("role", member.getRole());
                model.addAttribute("comment", commentService.findComment(boardRequestDto.getId()));
                boardService.updateBoardReadCntInc(boardRequestDto.getId());
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/view";
    }
}
