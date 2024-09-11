package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.comment.CommentResponseDto;
import spring.study.entity.Board;
import spring.study.entity.Comment;
import spring.study.entity.Member;
import spring.study.service.BoardService;
import spring.study.service.CommentService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@RequestMapping("/board")
public class BoardViewController {
    private final BoardService boardService;
    private Member member;
    private Long previous = -1L;
    private Board board;
    private List<Comment> list;
    private HashMap<String, Object> comment;

    @GetMapping("/list")
    public String getBoardListPage(Model model,
                                   HttpSession session,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size) throws Exception {
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");



        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        try {
            model.addAttribute("resultMap", boardService.findAll(page, size));
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/list";
    }

    @GetMapping("/write")
    public String getBoardWritePage(Model model, HttpSession session){
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        model.addAttribute("name", member.getName());
        return "/board/write";
    }

    @GetMapping("/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto, HttpSession session) {
        if (session == null)
            return "redirect:/member/login?error=true&exception=Session Expired";

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (boardRequestDto.getId() != null) {
            board = boardService.findById(boardRequestDto.getId());

            comment = new HashMap<>();
            list = board.getComment();

            comment.put("list", list.stream().map(CommentResponseDto::new).toList());

            if (!previous.equals(boardRequestDto.getId())) {
                boardService.updateBoardReadCntInc(boardRequestDto.getId());
                previous = boardRequestDto.getId();
            }

            model.addAttribute("info", board);
            model.addAttribute("email", member.getEmail());
            model.addAttribute("role", member.getRole());
            model.addAttribute("comment", comment);
        }

        return "/board/view";
    }
}
