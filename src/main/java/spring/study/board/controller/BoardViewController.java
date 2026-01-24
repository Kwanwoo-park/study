package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.entity.Board;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.board.service.BoardService;
import spring.study.member.service.MemberService;

@RequiredArgsConstructor
@Controller
@RequestMapping("/board")
@Slf4j
public class BoardViewController {
    private final BoardService boardService;
    private final MemberService memberService;

    @GetMapping("/all")
    public String getBoardListPage(Model model,
                                   @RequestParam(required = false, defaultValue = "") String title,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size,
                                   HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();

        Member member = (Member) session.getAttribute("member");

        if (member.getPhone().equals(" ")) {
            return "redirect:/member/updatePhone";
        }

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        try {
            model.addAttribute("resultMap", boardService.findAll(page, size));
            model.addAttribute("member", member);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "board/list";
    }

    @GetMapping("/main")
    public String mainPage(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        Member member = (Member) session.getAttribute("member");

        if (member.getPhone().equals(" ") || member.getBirth().equals("1900-01-01")) {
            return "redirect:/member/updatePhone";
        }

        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "board/main";
    }

    @GetMapping("/write")
    public String getBoardWritePage(Model model, HttpServletRequest request){
        HttpSession session = request.getSession();

        Member member = (Member) session.getAttribute("member");

        model.addAttribute("name", member.getName());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "board/write";
    }

    @GetMapping("/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        Member member = (Member) session.getAttribute("member");

        if (boardService.existBoard(boardRequestDto.getId())) {
            Board board = boardService.findById(boardRequestDto.getId());
            long[] ids = boardService.getBoardIdList(boardRequestDto.getId(), board.getMember());

            model.addAttribute("board", board);
            model.addAttribute("like", member.checkFavorite(board));
            model.addAttribute("member", member.getEmail());
            model.addAttribute("previous", ids[0]);
            model.addAttribute("next", ids[1]);
        } else {
            return "redirect:/member/detail?email="+member.getEmail();
        }

        return "board/view";
    }
}
