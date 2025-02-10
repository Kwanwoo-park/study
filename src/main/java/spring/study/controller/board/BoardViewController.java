package spring.study.controller.board;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.comment.CommentResponseDto;
import spring.study.entity.board.Board;
import spring.study.entity.board.BoardImg;
import spring.study.entity.comment.Comment;
import spring.study.entity.member.Member;
import spring.study.service.board.BoardImgService;
import spring.study.service.board.BoardService;

import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/board")
@Slf4j
public class BoardViewController {
    private final BoardService boardService;
    private final BoardImgService boardImgService;
    private Member member;
    private Long previous = -1L;

    @GetMapping("/all")
    public String getBoardListPage(Model model,
                                   @RequestParam(required = false, defaultValue = "") String title,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size,
                                   HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (member.getPhone().equals(" ")) {
            return "redirect:/member/updatePhone";
        }

        try {
            if (title.equals(""))
                model.addAttribute("resultMap", boardService.findAll(page, size));
            else {
                model.addAttribute("title", title);
                model.addAttribute("resultMap", boardService.findByTitle(title, page, size));
            }
            model.addAttribute("member", member);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "board/all";
    }

    @GetMapping("/list")
    public String getBoardPage(Model model,
                               @RequestParam(required = false, defaultValue = "0") Integer page,
                               @RequestParam(required = false, defaultValue = "5") Integer size,
                               HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (member.getPhone().equals(" ")) {
            return "redirect:/member/updatePhone";
        }

        try {
            model.addAttribute("resultMap", boardService.findByMembers(member, page, size));
            model.addAttribute("member", member);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "board/list";
    }

    @GetMapping("/main")
    public String mainPage(Model model, HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();;

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        try {
            model.addAttribute("resultMap", boardService.findByMembers(member));
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "board/main";
    }

    @GetMapping("/write")
    public String getBoardWritePage(Model model, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        model.addAttribute("name", member.getName());
        return "board/write";
    }

    @GetMapping("/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (boardRequestDto.getId() != null) {
            Board board = boardService.findById(boardRequestDto.getId());
            List<BoardImg> img = boardImgService.findBoard(board);

            HashMap<String, Object> comment = new HashMap<>();
            List<Comment> list = board.getComment();

            comment.put("list", list.stream().map(CommentResponseDto::new).toList());

            if (!previous.equals(boardRequestDto.getId())) {
                boardService.updateBoardReadCntInc(boardRequestDto.getId());
                previous = boardRequestDto.getId();
            }

            model.addAttribute("info", board);
            model.addAttribute("member", member);
            model.addAttribute("comment", comment);
            model.addAttribute("size", list.size());
            model.addAttribute("img", img);
        }

        return "board/view";
    }
}
