package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.board.BoardRequestDto;
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
                                   HttpSession session,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size) throws Exception {
        if (session == null)
            return "redirect:/member/login?error=true&exception=Session Expired";

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
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        model.addAttribute("name", member.getName());
        return "/board/write";
    }

    @GetMapping("/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto, HttpSession session) throws Exception {
        try {
            if (session == null)
                return "redirect:/member/login?error=true&exception=Session Expired";

            if (member == null) {
                session.invalidate();
                return "redirect:/member/login?error=true&exception=Login Please";
            }

            member = (Member) session.getAttribute("member");

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
