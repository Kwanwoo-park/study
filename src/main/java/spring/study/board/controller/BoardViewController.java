package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.entity.Board;
import spring.study.common.service.SessionService;
import spring.study.member.entity.Member;
import spring.study.board.service.BoardService;

@RequiredArgsConstructor
@Controller
@RequestMapping("/board")
@Slf4j
public class BoardViewController {
    private final BoardService boardService;
    private final SessionService sessionService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public String getBoardListPage(Model model,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size,
                                   HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        model.addAttribute("resultMap", boardService.findAll(page, size));
        model.addAttribute("member", member);

        return "board/list";
    }

    @GetMapping("/main")
    public String mainPage(Model model, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "board/main";
    }

    @GetMapping("/write")
    public String getBoardWritePage(Model model, HttpServletRequest request){
        Member member = sessionService.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        model.addAttribute("name", member.getName());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("email", member.getEmail());

        return "board/write";
    }

    @GetMapping("/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

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
